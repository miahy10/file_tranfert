import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {
    private static final Map<String, String> properties = new HashMap<>();

    static {
        try (BufferedReader br = new BufferedReader(new FileReader("config.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=", 2); // Sépare la clé et la valeur
                if (parts.length == 2) {
                    properties.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du fichier de configuration : " + e.getMessage());
        }
    }

    public static String get(String key) {
        return properties.get(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(properties.get(key));
    }

    public ConfigLoader() {
    }
}
