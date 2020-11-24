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
import java.nio.charset.StandardCharsets;


/**
 * @author pkern01
 */
public class Client {

    private final String ipAddress;
    private int port;
    private String username;
    private String password;
    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;
    private boolean connected;
    private ClientUI ui;

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

    public Client(String ipAddress, int port, String username, String password, ClientUI ui) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
        this.password = password;
        this.connected = false;
        this.ui = ui;
    }

    public void connect() throws ConnectException, UnknownHostException, IOException {
        this.socket = new Socket(ipAddress, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        sendMessageAndGetResponse("::username=" + username);
        String response = sendMessageAndGetResponse("::password=" + password);
        if (response.startsWith("bad-auth")) {
            connected = false;
            ui.receiveData("Identifiants incorrects !");
            ui.clientDisconnected();
        } else {
            connected = true;
        }

    }

    public String sendMessageAndGetResponse(String message) {
        try {
            out.write(message + "\n");
            out.flush();
            if (message.equals("::close")) {
                disconnect();
                return null;
            }

            return in.readLine().replace("//", "\n");
        } catch (IOException ex) {
            ui.receiveData("[ERROR] Erreur lors de la lecture ou de l'écriture des flux.");
        }
        return null;
    }

    public void disconnect() {
        try {
            ui.clientDisconnected();
            socket.close();
            connected = false;
        } catch (IOException ex) {
        }
    }

}
