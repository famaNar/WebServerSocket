import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class MesRequettes {
    private Socket socketClient; 
    // Constructeur prenant en paramètre un socket client
    public MesRequettes(Socket socket) {
        this.socketClient = socket; 
    }

    // traitement des requêtes du client
    public void traitements() {
        try (BufferedReader entree = new BufferedReader(new InputStreamReader(socketClient.getInputStream(), "UTF-8"));
             BufferedWriter sortie = new BufferedWriter(new OutputStreamWriter(socketClient.getOutputStream(), "UTF-8"))) {

            String ligne = entree.readLine();
            if (ligne != null && !ligne.isEmpty()) {

                String[] tokens = ligne.split(" ");
                if (tokens.length >= 3 && tokens[0].equals("GET")) {
                    String cheminRelatif = tokens[1]; // Récupération du chemin relatif demandé

                    // Détermination du chemin  demandé dans le repertoire racine
                    Path repertoireRacine = Paths.get("repertoire");
                    Path cheminDemande = repertoireRacine.resolve(cheminRelatif.substring(1)).normalize();

                    // Vérification  du chemin demandé
                    if (!cheminDemande.startsWith(repertoireRacine)) {
                        // Si le chemin  n'est pas en dehors du repertoire racine, on renvoi une erreur 403
                        sortie.write("HTTP/1.1 403 Forbidden\r\n\r\n<h1>403 Forbidden</h1>");
                    } else if (Files.exists(cheminDemande)) {
                        // Si le chemin existe
                        if (Files.isDirectory(cheminDemande)) {
                            // Si on a un répertoire, on liste son contenu 
                            List<Path> paths = Files.list(cheminDemande).collect(Collectors.toList());
                            String pathsList = paths.stream()
                                    .map(repertoireRacine::relativize)
                                    .map(path -> "<li><a href=\"/" + path.toString() + "\">" + path.getFileName() + "</a></li>")
                                    .collect(Collectors.joining());

                            String htmlResponse = "<!DOCTYPE html>\n" +
                                    "<html lang=\"en\">\n" +
                                    "<head>\n" +
                                    "    <meta charset=\"UTF-8\">\n" +
                                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                                    "    <title>Liste des fichiers</title>\n" +
                                    "    <style>\n" +
                                    "        body {\n" +
                                    "            font-family: Arial, sans-serif;\n" +
                                    "            margin: 0;\n" +
                                    "            padding: 0;\n" +
                                    "        }\n" +
                                    "        .container {\n" +
                                    "            max-width: 800px;\n" +
                                    "            margin: 20px auto;\n" +
                                    "            padding: 20px;\n" +
                                    "            border: 1px solid #ccc;\n" +
                                    "            border-radius: 5px;\n" +
                                    "        }\n" +
                                    "        h1 {\n" +
                                    "            text-align: center;\n" +
                                    "            margin-bottom: 20px;\n" +
                                    "        }\n" +
                                    "        ul {\n" +
                                    "            list-style-type: none;\n" +
                                    "            padding: 0;\n" +
                                    "        }\n" +
                                    "        li {\n" +
                                    "            margin-bottom: 10px;\n" +
                                    "            background-color: #f9f9f9;\n" +
                                    "            padding: 10px;\n" +
                                    "            border-radius: 5px;\n" +
                                    "        }\n" +
                                    "        a {\n" +
                                    "            text-decoration: none;\n" +
                                    "            color: #333;\n" +
                                    "            font-weight: bold;\n" +
                                    "        }\n" +
                                    "        a:hover {\n" +
                                    "            color: #666;\n" +
                                    "        }\n" +
                                    "    </style>\n" +
                                    "</head>\n" +
                                    "<body>\n" +
                                    "    <div class=\"container\">\n" +
                                    "        <h1>Liste des fichiers et répertoires/sous repertoires</h1>\n" +
                                    "        <ul>" + pathsList + "</ul>\n" +
                                    "    </div>\n" +
                                    "</body>\n" +
                                    "</html>";

                            sortie.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n" + htmlResponse);
                        } else if (cheminDemande.toString().endsWith(".py")) {
                            // Si on a un fichier Python
                            try {
                                ProcessBuilder pb = new ProcessBuilder("/usr/bin/python3", cheminDemande.toString());
                                Process p = pb.start();

                                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                String line;
                                StringBuilder pythonOutput = new StringBuilder();
                                while ((line = br.readLine()) != null) {
                                    pythonOutput.append(line).append(System.lineSeparator());
                                }

                                int exitCode = p.waitFor(); 
                                if (exitCode == 0) {
                                    sortie.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n");
                                    sortie.write(pythonOutput.toString());
                                } else {
                                    //si le script Python échoue il y'aura erreur
                                    sortie.write("HTTP/1.1 500 Internal Server Error\r\n\r\n<h1>500 Internal Server Error</h1>");
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt(); 
                                sortie.write("HTTP/1.1 500 Internal Server Error\r\n\r\n<h1>500 Internal Server Error</h1>");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // Si c'est un fichier
                            byte[] contenuFichier = Files.readAllBytes(cheminDemande);
                            String typeMIME = Files.probeContentType(cheminDemande);

                            sortie.write("HTTP/1.1 200 OK\r\nContent-Type: " + typeMIME + "; charset=UTF-8\r\nContent-Length: " + contenuFichier.length + "\r\n\r\n");
                            sortie.flush();

                            OutputStream sortieFichier = socketClient.getOutputStream();
                            sortieFichier.write(contenuFichier, 0, contenuFichier.length);
                            sortieFichier.flush();
                        }
                    } else {
                        // Si le chemin n'existe pas, il renvoi  une erreur 404
                        sortie.write("HTTP/1.1 404 Not Found\r\n\r\n<h1>404 Not Found</h1>");
                    }
                    sortie.flush(); //Le  client recois
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socketClient.close(); //socket client ferme 
            } catch (IOException e) {
                e.printStackTrace(); 
            }
        }
    }
}
