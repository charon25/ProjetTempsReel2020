/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.insa.kern.projet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


// Classe qui permet de gérer un client
public class Client {

    private final String ipAddress; // Adresse IP du serveur
    private int port; // Port du serveur
    private String username; // Nom du client
    private String password; // Mot de passe du client
    private BufferedReader in; // Buffer de lecture
    private PrintWriter out; // Buffer d'écriture
    private Socket socket; // Socket de connexion
    private boolean connected; // Indique si le client est connecté
    private ClientUI ui; // Interface graphique associée

    // ACCESSEURS
    public String getIpAddress() {
        return ipAddress;
    }
    public int getPort() {
        return port;
    }
    public BufferedReader getInReader() {
        return in;
    }
    public PrintWriter getOutWriter() {
        return out;
    }
    public Socket getSocket() {
        return socket;
    }
    public String getUserName() {
        return username;
    }
    public boolean isConnected() {
        return connected;
    }

    // Instancie un client à l'aide d'une adresse IP, d'un port, d'un nom, d'un mot de passe et d'une interface graphique
    public Client(String ipAddress, int port, String username, String password, ClientUI ui) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
        this.password = password;
        this.connected = false;
        this.ui = ui;
    }

    // Connecte le client, et renvoie les exceptions possibles
    public void connect() throws ConnectException, UnknownHostException, IOException, NoSuchAlgorithmException {
        // On instancie le socket et les buffers
        this.socket = new Socket(ipAddress, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        // On envoie deux messages au serveur pour indiquer le nom et le mot de passe hashé
        sendMessageAndGetResponse("::username=" + username);
        String response = sendMessageAndGetResponse("::password=" + Login.getHashString(password));

        // Si l'authentification est mauvaise, on l'indique, sinon on indique qu'on est connecté
        if (response.startsWith("bad-auth")) {
            connected = false;
            ui.receiveData("Identifiants incorrects !");
            ui.clientDisconnected();
        } else {
            connected = true;
        }

    }

    // Envoie un message et attend une réponse
    public String sendMessageAndGetResponse(String message) {
        try {
            // On écrit le message
            out.write(message + "\n");
            out.flush();
            // Si le message est "::close" on déconnecte le client
            if (message.equals("::close")) {
                disconnect();
                return null;
            }
            // On lit la réponse du serveur et on remplace // par des retours à la ligne
            return in.readLine().replace("// ", "\n");
        } catch (IOException ex) { // Si il y a eu une erreur, on l'indique sur l'interface
            ui.receiveData("[ERROR] Erreur lors de la lecture ou de l'écriture des flux.");
        }
        return null;
    }

    // Déconnecte le client
    public void disconnect() {
        try {
            ui.clientDisconnected();
            socket.close();
            connected = false;
        } catch (IOException ex) {}
    }

}
