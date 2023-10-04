package Chat;

import java.io.*;
import java.net.*;
public class ClientWhatsCopernic {
    public static void main(String[] args) throws IOException {

        // Crea un socket de servidor
        ServerSocket serverSocket = new ServerSocket(42069);
        System.out.println("Conexion con WhatsCopernic.... Realizada!!");

        while (true) {

            // Acepta la conexión del usuario
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            // Mensaje de bienvenida
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = in.readLine();
            System.out.println("Client says: " + message);

        }

    }
}
