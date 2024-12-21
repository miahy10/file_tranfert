import java.io.*;
import java.net.Socket;

public class Client {
    private static final int SERVER_PORT = 12345;
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int CHUNK_SIZE = 1024;

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            try {
                System.out.print("Entrez une commande (GET, PUT, ls, RM, exit) : ");
                String input = reader.readLine().trim();

                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                String[] parts = input.split(" ");
                switch (parts[0].toUpperCase()) {
                    case "GET":
                        if (parts.length < 2) {
                            System.out.println("Usage : GET <nom de fichier> <chemin destination>");
                        } else {
                            receiveFile(parts[1], parts[2]);
                        }
                        break;
                    case "PUT":
                        if (parts.length < 2) {
                            System.out.println("Usage : PUT <chemin fichier>");
                        } else {
                            sendFile(parts[1]);
                        }
                        break;
                    case "LS":
                        listFiles();
                        break;
                    case "RM":
                        if (parts.length < 2) {
                            System.out.println("Usage : RM <nom de fichier>");
                        } else {
                            removeFile(parts[1]);
                        }
                        break;
                    default:
                        System.out.println("Commande inconnue.");
                }
            } catch (IOException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    private static void sendFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 FileInputStream fis = new FileInputStream(file)) {

                dos.writeUTF("ENVOYER");
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());

                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                System.out.println("Fichier envoyé : " + file.getName());
            } catch (IOException e) {
                System.out.println("Erreur lors de l'envoi : " + e.getMessage());
            }
        } else {
            System.out.println("Fichier non valide : " + filePath);
        }
    }

    private static void receiveFile(String fileName, String destinationPath) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {
    
            // Envoyer une requête pour recevoir un fichier
            dos.writeUTF("RECEVOIR");
            dos.writeUTF(fileName);
    
            // Lire la réponse du serveur
            String response = dis.readUTF();
            if ("OK".equals(response)) {
                long fileSize = dis.readLong();
                File destinationFile = new File(destinationPath);
    
                // Vérifier si le chemin est valide et peut être créé
                if (destinationFile.isDirectory()) {
                    destinationFile = new File(destinationFile, fileName);
                } else if (destinationFile.getParentFile() != null && !destinationFile.getParentFile().exists()) {
                    if (!destinationFile.getParentFile().mkdirs()) {
                        System.out.println("Erreur : Impossible de créer le répertoire de destination.");
                        return;
                    }
                }
    
                // Lire les données du fichier
                try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
                    byte[] buffer = new byte[CHUNK_SIZE];
                    long totalRead = 0;
                    int bytesRead;
                    while (totalRead < fileSize) {
                        bytesRead = dis.read(buffer, 0, (int) Math.min(CHUNK_SIZE, fileSize - totalRead));
                        if (bytesRead == -1) {
                            throw new IOException("Fin prématurée de la connexion au serveur.");
                        }
                        fos.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                    }
                }
    
                System.out.println("Fichier reçu avec succès : " + destinationFile.getAbsolutePath());
            } else {
                System.out.println("Erreur : " + dis.readUTF());
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la réception : " + e.getMessage());
        }
    }
    
    

    private static void listFiles() {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {
    
            dos.writeUTF("LISTER");
            int fileCount = dis.readInt();
    
            if (fileCount > 0) {
                System.out.println("Fichiers disponibles : ");
                for (int i = 0; i < fileCount; i++) {
                    System.out.println(dis.readUTF());
                }
            } else {
                System.out.println("Aucun fichier disponible ou tous les sous-serveurs sont hors ligne.");
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la liste des fichiers : " + e.getMessage());
        }
    }
    
    private static void removeFile(String fileName) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("RM");
            dos.writeUTF(fileName);

            String response = dis.readUTF();
            if ("OK".equalsIgnoreCase(response)) {
                System.out.println("Fichier supprimé avec succès : " + fileName);
            } else {
                System.out.println("Erreur lors de la suppression : " + response);
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la commande RM : " + e.getMessage());
        }
    }
}
