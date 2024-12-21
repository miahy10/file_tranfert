import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainServer {
    private static final ConfigLoader config = new ConfigLoader();
    private static final int PORT = config.getInt("main_server.port");
    private static final String SERVER_DIRECTORY = config.get("main_server.directory");
    private static final int CHUNK_SIZE = config.getInt("chunk_size");

    private static final List<SubServerInfo> SUB_SERVERS = new ArrayList<>();

    static {
    int subServerCount = config.getInt("sub_server.count");
    int minSubServerCount = 1;
    int connectedSubServers = 0;

    for (int i = 1; i <= subServerCount; i++) {
        String host = config.get("sub_server." + i + ".host");
        int port = config.getInt("sub_server." + i + ".port");

        try (Socket socket = new Socket(host, port)) {
            SUB_SERVERS.add(new SubServerInfo(host, port));
            connectedSubServers++;
        } catch (IOException e) {
            System.err.println("Sous-serveur inaccessible : " + host + ":" + port);
        }
    }

    if (connectedSubServers < minSubServerCount) {
        System.err.println("Erreur : moins de " + minSubServerCount + " sous-serveurs disponibles.");
        System.exit(1);
    }
}

    public static void main(String[] args) {
        File serverDir = new File(SERVER_DIRECTORY);
        if (!serverDir.exists()) {
            serverDir.mkdirs();
        }

        ExecutorService threadPool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur principal en écoute sur le port " + PORT + "...");
            for (SubServerInfo subServer : SUB_SERVERS) {
                try (Socket socket = new Socket(subServer.host, subServer.port)) {
                    System.out.println("Sous-serveur connecté : " + subServer.host + ":" + subServer.port); // Notification
                }
            }
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté : " + clientSocket.getInetAddress());

                threadPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket;
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            String command = dis.readUTF();
            if ("ENVOYER".equalsIgnoreCase(command)) {
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                distributeAndReplicateFile(dis, fileName, fileSize);
                System.out.println("Fichier reçu et distribué : " + fileName);
            } else if ("RECEVOIR".equalsIgnoreCase(command)) {
                String fileName = dis.readUTF();
                File assembledFile = assembleFile(fileName);
                sendFileInChunks(assembledFile, dos);
                if (assembledFile.delete()) {
                    System.out.println("Fichier supprimé après envoi : " + fileName);
                } else {
                    System.err.println("Erreur lors de la suppression du fichier : " + fileName);
                }

            } else if ("LISTER".equalsIgnoreCase(command)) {
                Set<String> uniqueFileNames = new HashSet<>();
                int accessibleSubServers = 0;

                for (SubServerInfo subServer : SUB_SERVERS) {
                    try (Socket subSocket = new Socket(subServer.host, subServer.port);
                            DataOutputStream subDos = new DataOutputStream(subSocket.getOutputStream());
                            DataInputStream subDis = new DataInputStream(subSocket.getInputStream())) {

                        subDos.writeUTF("LISTER");

                        int fileCount = subDis.readInt();
                        System.out.println(
                                "Sous-serveur " + subServer.host + " a répondu avec " + fileCount + " fichiers."); // Notification

                        for (int i = 0; i < fileCount; i++) {
                            String fileName = subDis.readUTF();
                            uniqueFileNames.add(fileName.replaceAll("\\.part\\d+$", ""));
                        }
                        accessibleSubServers++;
                    } catch (IOException e) {
                        System.err.println("Sous-serveur inaccessible : " + subServer.host + ":" + subServer.port);
                    }
                }

                if (accessibleSubServers > 0) {
                    dos.writeInt(uniqueFileNames.size());
                    for (String fileName : uniqueFileNames) {
                        dos.writeUTF(fileName);
                    }
                } else {
                    dos.writeInt(0); // Aucun fichier listé
                }
            }

            else if ("RM".equalsIgnoreCase(command)) {
                String fileName = dis.readUTF();

                // Get the list of active sub-servers
                List<SubServerInfo> activeSubServers = getActiveSubServers();

                if (!activeSubServers.isEmpty()) {
                    // Delete the file from the active sub-servers
                    deleteFileFromActiveSubServers(fileName, activeSubServers, dos);
                    dos.writeUTF("Fichier supprimé avec succès des serveurs actifs.");
                } else {
                    dos.writeUTF("Échec : Aucun sous-serveur actif.");
                }
            }

            else {
                dos.writeUTF("COMMANDE INCONNUE");
            }
        } catch (IOException e) {
            System.err.println("Erreur avec un client : " + e.getMessage());
        }
    }

    private static boolean checkAllSubServersOnline() {
        for (SubServerInfo subServer : SUB_SERVERS) {
            try (Socket socket = new Socket(subServer.host, subServer.port)) {
                // Si la connexion réussit, le sous-serveur est en ligne.
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    private static List<SubServerInfo> getActiveSubServers() {
        List<SubServerInfo> activeSubServers = new ArrayList<>();
        for (SubServerInfo subServer : SUB_SERVERS) {
            try (Socket socket = new Socket(subServer.host, subServer.port)) {
                // If the connection succeeds, the sub-server is active
                activeSubServers.add(subServer);
                System.out.println("Sous-serveur connecté : " + subServer.host + ":" + subServer.port); // Notification
            } catch (IOException e) {
                // Sub-server is not reachable, so it's not considered active
                System.err.println("Sous-serveur inaccessible : " + subServer.host + ":" + subServer.port);
            }
        }
        return activeSubServers;
    }

    private static void deleteFileFromActiveSubServers(String fileName, List<SubServerInfo> activeSubServers,
            DataOutputStream dos) throws IOException {
        boolean success = true;

        for (SubServerInfo subServer : activeSubServers) {
            try (Socket subSocket = new Socket(subServer.host, subServer.port);
                    DataOutputStream subDos = new DataOutputStream(subSocket.getOutputStream());
                    DataInputStream subDis = new DataInputStream(subSocket.getInputStream())) {

                // Send the DELETE command to the sub-server
                subDos.writeUTF("DELETE");
                subDos.writeUTF(fileName);

                // Read the response from the sub-server
                String response = subDis.readUTF();
                if (!"OK".equalsIgnoreCase(response)) {
                    System.err
                            .println("Erreur lors de la suppression du fichier " + fileName + " sur " + subServer.host);
                    success = false;
                }
            } catch (IOException e) {
                success = false;
                System.err.println("Erreur de connexion au sous-serveur " + subServer.host);
            }
        }

        if (success) {
            dos.writeUTF("OK");
            System.out.println("Fichier " + fileName + " supprimé des sous-serveurs actifs.");
        } else {
            dos.writeUTF("ERREUR");
            System.err.println("Échec de la suppression du fichier " + fileName + " sur certains sous-serveurs.");
        }
    }

    private static void deleteFileFromAllSubServers(String fileName, DataOutputStream dos) throws IOException {
        boolean success = true;

        for (SubServerInfo subServer : SUB_SERVERS) {
            try (Socket subSocket = new Socket(subServer.host, subServer.port);
                    DataOutputStream subDos = new DataOutputStream(subSocket.getOutputStream());
                    DataInputStream subDis = new DataInputStream(subSocket.getInputStream())) {

                // Envoyer la commande de suppression pour tous les fragments associés au
                // fichier.
                subDos.writeUTF("DELETE_ALL_FRAGMENTS");
                subDos.writeUTF(fileName);

                // Lire la réponse du sous-serveur.
                String response = subDis.readUTF();
                if (!"OK".equalsIgnoreCase(response)) {
                    System.err.println(
                            "Erreur lors de la suppression des fragments de " + fileName + " sur " + subServer.host);
                    success = false;
                }
            } catch (IOException e) {
                success = false;
                System.err.println("Erreur de connexion au sous-serveur " + subServer.host);
            }
        }

        if (success) {
            dos.writeUTF("OK");
            System.out.println("Tous les fragments du fichier " + fileName + " ont été supprimés avec succès.");
        } else {
            dos.writeUTF("ERREUR");
            System.err.println("Échec de la suppression complète des fragments du fichier " + fileName + ".");
        }
    }

    private static void distributeAndReplicateFile(DataInputStream dis, String fileName, long fileSize)
            throws IOException {
        long fragmentSize = fileSize / SUB_SERVERS.size();
        byte[] buffer = new byte[CHUNK_SIZE];

        for (int i = 0; i < SUB_SERVERS.size(); i++) {
            long bytesToSend = (i == SUB_SERVERS.size() - 1) ? (fileSize - i * fragmentSize) : fragmentSize;

            try (Socket subServerSocket = new Socket(SUB_SERVERS.get(i).host, SUB_SERVERS.get(i).port);
                    DataOutputStream subDos = new DataOutputStream(subServerSocket.getOutputStream())) {

                subDos.writeUTF("STORE");
                subDos.writeUTF(fileName + ".part" + i);
                subDos.writeLong(bytesToSend);

                long bytesSent = 0;
                while (bytesSent < bytesToSend) {
                    int bytesRead = dis.read(buffer, 0, (int) Math.min(CHUNK_SIZE, bytesToSend - bytesSent));
                    subDos.write(buffer, 0, bytesRead);
                    bytesSent += bytesRead;
                }

                System.out.println(
                        "Fichier fragmenté envoyé à " + SUB_SERVERS.get(i).host + ":" + SUB_SERVERS.get(i).port); // Notification
            }

            replicateToOtherSubServers(fileName + ".part" + i, i);
        }
    }

    private static void replicateToOtherSubServers(String fileName, int sourceSubServerIndex) {
        for (int j = 0; j < SUB_SERVERS.size(); j++) {
            if (j != sourceSubServerIndex) {
                try (Socket sourceSocket = new Socket(SUB_SERVERS.get(sourceSubServerIndex).host,
                        SUB_SERVERS.get(sourceSubServerIndex).port);
                        Socket targetSocket = new Socket(SUB_SERVERS.get(j).host, SUB_SERVERS.get(j).port);
                        DataOutputStream sourceDos = new DataOutputStream(sourceSocket.getOutputStream());
                        DataInputStream sourceDis = new DataInputStream(sourceSocket.getInputStream());
                        DataOutputStream targetDos = new DataOutputStream(targetSocket.getOutputStream())) {

                    sourceDos.writeUTF("RETRIEVE");
                    sourceDos.writeUTF(fileName);

                    long fragmentSize = sourceDis.readLong();
                    targetDos.writeUTF("STORE");
                    targetDos.writeUTF(fileName);
                    targetDos.writeLong(fragmentSize);

                    byte[] buffer = new byte[CHUNK_SIZE];
                    long bytesSent = 0;
                    while (bytesSent < fragmentSize) {
                        int bytesRead = sourceDis.read(buffer, 0, (int) Math.min(CHUNK_SIZE, fragmentSize - bytesSent));
                        targetDos.write(buffer, 0, bytesRead);
                        bytesSent += bytesRead;
                    }

                    System.out.println("Fragment " + fileName + " répliqué à " + SUB_SERVERS.get(j).host + ":"
                            + SUB_SERVERS.get(j).port); // Notification
                } catch (IOException e) {
                    System.err.println("Erreur de réplication sur le sous-serveur " + SUB_SERVERS.get(j).host);
                }
            }
        }
    }

    private static File assembleFile(String fileName) throws IOException {
        File assembledFile = new File(SERVER_DIRECTORY, fileName);

        try (FileOutputStream fos = new FileOutputStream(assembledFile)) {
            byte[] buffer = new byte[CHUNK_SIZE];

            for (int i = 0; i < SUB_SERVERS.size(); i++) {
                boolean fragmentRetrieved = false;

                for (int attempt = 0; attempt < 3; attempt++) {
                    int serverIndex = (i + attempt) % SUB_SERVERS.size(); // Alterner les serveurs en cas d'erreur
                    SubServerInfo subServer = SUB_SERVERS.get(serverIndex);

                    try (Socket subServerSocket = new Socket(subServer.host, subServer.port);
                            DataOutputStream subDos = new DataOutputStream(subServerSocket.getOutputStream());
                            DataInputStream subDis = new DataInputStream(subServerSocket.getInputStream())) {

                        subDos.writeUTF("RETRIEVE");
                        subDos.writeUTF(fileName + ".part" + i);

                        long fragmentSize = subDis.readLong();
                        long bytesReceived = 0;
                        while (bytesReceived < fragmentSize) {
                            int bytesRead = subDis.read(buffer, 0,
                                    (int) Math.min(CHUNK_SIZE, fragmentSize - bytesReceived));
                            fos.write(buffer, 0, bytesRead);
                            bytesReceived += bytesRead;
                        }

                        fragmentRetrieved = true; // Fragment récupéré avec succès
                        break;
                    } catch (IOException e) {
                        System.err.println("Erreur avec le sous-serveur " + subServer.host + " pour le fragment "
                                + fileName + ".part" + i);
                    }
                }

                if (!fragmentRetrieved) {
                    throw new IOException("Impossible de récupérer le fragment " + fileName + ".part" + i
                            + " après plusieurs tentatives.");
                }
            }
        }

        return assembledFile;
    }

    private static void sendFileInChunks(File file, DataOutputStream dos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            dos.writeUTF("OK");
            dos.writeLong(file.length());
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }
    }

    private static class SubServerInfo {
        String host;
        int port;

        SubServerInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}
