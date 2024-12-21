import java.io.*;
import java.net.*;

public class SubServer {
    private static final int CHUNK_SIZE = 1024;
    

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]); // Le port est passé en paramètre
        File subServerDir = new File("server_directory/sub_server_directory_" + port);
        

        if (!subServerDir.exists()) {
            subServerDir.mkdirs();
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Sous-serveur démarré sur le port " + port + "...");
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                     DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

                    String command = dis.readUTF();
                    if ("STORE".equalsIgnoreCase(command)) {
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();
                        receiveFile(dis, new File(subServerDir, fileName), fileSize);
                        System.out.println("Fragment stocké : " + fileName);
                    } else if ("RETRIEVE".equalsIgnoreCase(command)) {
                        String fileName = dis.readUTF();
                        File file = new File(subServerDir, fileName);
                        sendFile(dos, file);
                        System.out.println("Fragment envoyé : " + fileName);
                    } else if ("LISTER".equalsIgnoreCase(command)) {
                        File[] files = subServerDir.listFiles();
                        if (files != null) {
                            dos.writeInt(files.length); // Envoyer le nombre de fichiers
                            for (File file : files) {
                                dos.writeUTF(file.getName()); // Envoyer les noms des fichiers
                            }
                        } else {
                            dos.writeInt(0); // Pas de fichiers
                            
                        }
                    }else if ("DELETE".equalsIgnoreCase(command)) {
                        String fileName = dis.readUTF();
                        File[] fragments = subServerDir.listFiles((dir, name) -> name.startsWith(fileName + ".part"));
                    
                        boolean allDeleted = true;
                        if (fragments != null) {
                            for (File fragment : fragments) {
                                if (!fragment.delete()) {
                                    allDeleted = false;
                                    System.err.println("Impossible de supprimer le fragment : " + fragment.getName());
                                }
                            }
                        }
                    
                        dos.writeUTF(allDeleted ? "OK" : "ERREUR");
                    }
                    
                    
                } catch (IOException e) {
                    System.err.println("Erreur avec un client : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur avec le sous-serveur : " + e.getMessage());
        }
    }

    private static void receiveFile(DataInputStream dis, File file, long fileSize) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            long totalRead = 0;
            int bytesRead;
            while (totalRead < fileSize) {
                bytesRead = dis.read(buffer, 0, (int) Math.min(CHUNK_SIZE, fileSize - totalRead));
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
        }
    }

    private static void sendFile(DataOutputStream dos, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            dos.writeLong(file.length());
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }
    }
}
