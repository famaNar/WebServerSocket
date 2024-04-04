import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MonServeur {
    private ServerSocket serverSocket; 

    // Méthode pour démarrer le serveur 
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port); 
        while (true) {
            Socket socketClient = serverSocket.accept(); 
            MesRequettes requettes = new MesRequettes(socketClient);
            requettes.traitements(); 
        }
    }
    // Méthode principale pour démarrer le serveur
    public static void main(String[] args) throws IOException {
        MonServeur server = new MonServeur(); 
        server.start(80); // on a specifier le port 80
        System.out.println("Le serveur s'est allumé");


    }
}

