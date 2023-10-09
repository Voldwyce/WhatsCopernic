package Chat;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientWhatsCopernic {
    public static Scanner sc = new Scanner(System.in);
    public static Socket sk;

    public static void main(String[] args) throws IOException {
        System.out.println("¡¡Bienvenido a WhatsCopernic!! ");
        boolean salir = false;

        while (!salir) {
            salir = iniciarApp();
        }

        System.out.println("Bienvenido a WhatsCopernic");
        boolean continuar = true;

        while (continuar) {
            System.out.println("");
            System.out.println("Menú de opciones");
            System.out.println("1. Listar usuarios");
            System.out.println("2. Enviar mensaje");
            System.out.println("3. Recibir mensaje");
            System.out.println("4. Enviar archivo");
            System.out.println("5. Ver archivos");
            System.out.println("6. Recibir archivo");
            System.out.println("7. Crear grupo");
            System.out.println("8. Gestionar grupo");
            System.out.println("9. Eliminar grupo");
            System.out.println("10. Configuración");
            System.out.println("11. Salir");

            System.out.print("Elija una opción: ");
            int opcion = sc.nextInt();
            sc.nextLine();

            switch (opcion) {
                case 1:
                    listarUsuarios();
                    break;
                case 11:
                    logout();
                    continuar = false;
                    break;
                default:
                    System.out.println("Opción inválida");
                    break;
            }
        }
    }

    public static boolean iniciarApp() throws IOException {
        System.out.println("1. Iniciar Sesión");
        System.out.println("2. Crear Cuenta");
        System.out.print("Elija una opción: ");
        int opcion = sc.nextInt();
        sc.nextLine();

        System.out.print("Usuario: ");
        String usuario = sc.nextLine();
        System.out.print("Contraseña: ");
        String password = sc.nextLine();

        sk = new Socket("localhost", 42069);

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
            System.out.println("Session iniciada.");
            return true;
        } else {
            System.out.println("Credenciales incorrectas o error al crear la cuenta.");
            return false;
        }
    }

    public static void listarUsuarios() {
        try {
            PrintWriter out = new PrintWriter(sk.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(sk.getInputStream()));

            out.println("listar");

            BufferedReader respuesta = new BufferedReader(new InputStreamReader(sk.getInputStream()));

            String userList = respuesta.readLine();

            if (userList.equals("Comando incorrecto")) {
                System.out.println(userList);
            } else {
                String[] usernames = userList.split(", ");
                System.out.println("Usuarios conectados: ");
                for (String username : usernames) {
                    if (!username.equals("null")) {
                        System.out.println(username);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logout() {
        try {
            PrintWriter out = new PrintWriter(sk.getOutputStream(), true);
            out.println("logout");
            sk.close();
            System.out.println("Cierre de sesión exitoso. Hasta luego.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
