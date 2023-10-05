package Chat;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientWhatsCopernic {

    public static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        System.out.println("Bienvenido a WhatsCopernic!! ");

        System.out.println("1. Iniciar Sesión");











        boolean salir = false;
        while (!salir) {
            salir = iniciarApp();
        }
    }

    public static boolean iniciarApp() throws IOException {
        // Mostrar opciones al usuario
        System.out.println("1. Iniciar Sesión");
        System.out.println("2. Crear Cuenta");
        System.out.print("Elija una opción: ");
        int opcion = sc.nextInt();
        sc.nextLine();

        System.out.print("Usuario: ");
        String usuario = sc.nextLine();
        System.out.print("Contraseña: ");
        String password = sc.nextLine();

        Socket sk = new Socket("localhost", 42069);

        PrintWriter out = new PrintWriter(sk.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(sk.getInputStream()));

        if (opcion == 1) {
            out.println("login " + usuario + " " + password);
        } else if (opcion == 2) {
            out.println("create " + usuario + " " + password);
        } else {
            System.out.println("Opción inválida");
            return false;
        }

        String respuesta = in.readLine();
        if (respuesta.equals("true")) {
            System.out.println("Operación exitosa. Saliendo del bucle.");
            return true;
        } else {
            System.out.println("Credenciales incorrectas o error al crear la cuenta.");
            return false;
        }
    }
}
