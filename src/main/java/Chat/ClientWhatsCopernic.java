package Chat;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Properties;

public class ClientWhatsCopernic {
    public static Scanner sc = new Scanner(System.in);
    public static Socket sk;
    public static DataInputStream in;
    public static DataOutputStream out;
    public static ClientConfiguration clientConfig;


    public static void main(String[] args) throws IOException {
        loadClientConfiguration();
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
            int opcion = verificarInput(sc);
            sc.nextLine();

            switch (opcion) {
                case 1:
                    listarUsuarios();
                    break;
                case 2:
                    enviarMensaje();
                    break;
                case 7:
                    crearGrupo();
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

    private static void crearGrupo() {
        try {
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("creargrupo " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Grupo creado con éxito");
            } else {
                System.out.println("Error al crear el grupo");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean iniciarApp() {
        try {


            System.out.println("1. Iniciar Sesión");
            System.out.println("2. Crear Cuenta");
            int opcion = verificarInput(sc);
            sc.nextLine();

            System.out.print("Usuario: ");
            String usuario = sc.nextLine();
            System.out.print("Contraseña: ");
            String password = sc.nextLine();

            sk = new Socket(clientConfig.ipServidor, clientConfig.portServidor);
            in = new DataInputStream(sk.getInputStream());
            out = new DataOutputStream(sk.getOutputStream());

            if (opcion == 1) {
                out.writeUTF("login " + usuario + " " + password);
            } else if (opcion == 2) {
                out.writeUTF("create " + usuario + " " + password);
            } else {
                System.out.println("Opción inválida");
                return false;
            }
            String respuesta = in.readUTF();
            if (respuesta.equals("true")) {
                System.out.println();
                System.out.println("Sesión iniciada.");
                return true;
            } else {
                System.out.println("Credenciales incorrectas o error al crear la cuenta.");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void listarUsuarios() {
        try {
            out.writeUTF("listar");
            String response = in.readUTF();

            if (response.equals("Comando incorrecto")) {
                System.out.println("Error al listar usuarios");
            } else {
                String[] usernames = response.split(", ");
                for (String username : usernames) {
                    if (!username.equals("null")) {
                        System.out.println(username);
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void enviarMensaje() {
        System.out.println("1. Mensaje a un usuario");
        System.out.println("2. Mensaje a un grupo");
        int opcion = verificarInput(sc);
        sc.nextLine();
        switch (opcion) {
            case 1:
                mensajeUsuario();
                break;
            case 2:
                mensajeGrupo();
                break;
            default:
                System.out.println("Opción inválida");
                break;
        }
    }

    public static void mensajeUsuario() {
        try {
            System.out.print("Ingrese el nombre del destinatario: ");
            String destinatario = sc.nextLine();
            System.out.print("Ingrese el mensaje: ");
            String mensaje = sc.nextLine();

            out.writeUTF("mensaje " + destinatario + " " + mensaje);

            String respuestaServidor = in.readUTF();
            System.out.println(respuestaServidor);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void mensajeGrupo() {
        try {
            System.out.print("Ingrese el nombre del grupo: ");
            String destinatario = sc.nextLine();
            System.out.print("Ingrese el mensaje: ");
            String mensaje = sc.nextLine();

            out.writeUTF("mensajeGrupo " + destinatario + " " + mensaje);

            String respuestaServidor = in.readUTF();
            System.out.println(respuestaServidor);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



        public static void logout() {
        try {
            DataOutputStream out = new DataOutputStream(sk.getOutputStream());
            out.writeUTF("logout");
            sk.close();
            System.out.println("Hasta luego!! ^^");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientConfiguration {
        public String nombreCliente;
        public int tamanoMaximoArchivo;
        public String ipServidor;
        public int portServidor;

    }

    private static void loadClientConfiguration() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("client.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Carga las variables del archivo de configuración
        clientConfig = new ClientConfiguration();
        clientConfig.nombreCliente = properties.getProperty("nombreCliente");
        clientConfig.tamanoMaximoArchivo = Integer.parseInt(properties.getProperty("tamanoMaximoArchivo"));
        clientConfig.ipServidor = properties.getProperty("ipServidor");
        clientConfig.portServidor = Integer.parseInt(properties.getProperty("portServidor"));

    }

    public static int verificarInput(Scanner sc) {
        System.out.print("Elije una opción: ");
        int opcion;
        while (true) {
            if (sc.hasNextInt()) {
                opcion = sc.nextInt();
                break;
            } else {
                System.out.print("Opción no válida. \nElije una opción:  ");
                sc.nextLine();
            }
        }
        return opcion;
    }

}
