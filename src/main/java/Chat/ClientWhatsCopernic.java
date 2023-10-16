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

    public static void main(String[] args) {
        loadClientConfiguration();
        System.out.println("¡¡Bienvenido a WhatsCopernic!! ");
        boolean salir = false;

        while (!salir) {
            salir = iniciarApp();
        }

        System.out.println("Bienvenido a WhatsCopernic");
        boolean continuar = true;

        while (continuar) {
            System.out.println();
            System.out.println("Menú de opciones");
            System.out.println("1. Listar usuarios");
            System.out.println("2. Enviar mensaje");
            System.out.println("3. Recibir mensaje");
            System.out.println("4. Enviar archivo");
            System.out.println("5. Recibir archivo");
            System.out.println("6. Crear grupo");
            System.out.println("7. Salir");
            int opcion = verificarInput(sc);

            switch (opcion) {
                case 1:
                    listarUsuarios();
                    break;
                case 2:
                    enviarMensaje();
                    break;
                case 3:
                    recibirMensaje();
                    break;
                case 4:
                    enviarArchivo();
                    break;
                case 5:
                    recibirArchivo();
                    break;
                case 6:
                    crearGrupo();
                    break;
                case 7:
                    salir = true;
                    continuar = false;
                    break;
            }
        }
    }

    public static boolean iniciarApp() {
        System.out.println("");
        System.out.println("Menú de inicio");
        System.out.println("1. Registrarse");
        System.out.println("2. Iniciar Sesión");
        System.out.println("3. Salir");
        int respuesta = verificarInput(sc);

        if (respuesta == 3) {
            return true;
        }

        System.out.println("Introduce el nombre de usuario: ");
        String usuario = sc.nextLine();

        System.out.println("Introduce la contraseña: ");
        String pswd = sc.nextLine();

        try {
            sk = new Socket(clientConfig.ipServidor, clientConfig.portServidor);
            in = new DataInputStream(sk.getInputStream());
            out = new DataOutputStream(sk.getOutputStream());
            out.writeUTF("login " + usuario + " " + pswd);
            String respuestaLogin = in.readUTF();

            if (respuestaLogin.equals("true")) {
                return true;
            } else {
                System.out.println("Credenciales incorrectas");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    public static void listarUsuarios() {
        try {
            sk = new Socket(clientConfig.ipServidor, clientConfig.portServidor);
            in = new DataInputStream(sk.getInputStream());
            out = new DataOutputStream(sk.getOutputStream());
            out.writeUTF("listar");
            String response = in.readUTF();
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void enviarMensaje() {
        System.out.println("Introduce el nombre del destinatario: ");
        String destinatario = sc.nextLine();
        System.out.println("Introduce el mensaje: ");
        String mensaje = sc.nextLine();

        try {
            sk = new Socket(clientConfig.ipServidor, clientConfig.portServidor);
            in = new DataInputStream(sk.getInputStream());
            out = new DataOutputStream(sk.getOutputStream());
            out.writeUTF("mensaje " + destinatario + " " + mensaje);
            String response = in.readUTF();
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void recibirMensaje() {
        try {
            sk = new Socket(clientConfig.ipServidor, clientConfig.portServidor);
            in = new DataInputStream(sk.getInputStream());
            out = new DataOutputStream(sk.getOutputStream());
            out.writeUTF("mensajegrupo");
            String response = in.readUTF();
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void enviarArchivo() {
        System.out.println("Introduce la ruta del archivo a enviar: ");
        String rutaArchivo = sc.nextLine();
        File file = new File(rutaArchivo);

        if (!file.exists()) {
            System.out.println("El archivo no existe");
            return;
        }

        if (file.length() > clientConfig.tamanoMaximoArchivo) {
            System.out.println("El archivo excede el tamaño máximo permitido");
            return;
        }

        System.out.println("Introduce el nombre del destinatario: ");
        String destinatario = sc.nextLine();

        try {
            sk = new Socket(clientConfig.ipServidor, clientConfig.portServidor);
            in = new DataInputStream(sk.getInputStream());
            out = new DataOutputStream(sk.getOutputStream());
            out.writeUTF("archivo " + destinatario);
            FileInputStream fis = new FileInputStream(rutaArchivo);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }

            fis.close();
            out.close();
            sk.close();
            System.out.println("Archivo enviado con éxito");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void recibirArchivo() {
        try {
            sk = new Socket(clientConfig.ipServidor, clientConfig.portServidor);
            in = new DataInputStream(sk.getInputStream());
            out = new DataOutputStream(sk.getOutputStream());
            out.writeUTF("descargar " + clientConfig.rutaDescargaArchivos);
            FileOutputStream fos = new FileOutputStream(clientConfig.rutaDescargaArchivos);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.close();
            out.close();
            sk.close();
            System.out.println("Archivo recibido con éxito");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void crearGrupo() {
        System.out.println("Introduce el nombre del grupo: ");
        String nombreGrupo = sc.nextLine();

        try {
            sk = new Socket(clientConfig.ipServidor, clientConfig.portServidor);
            in = new DataInputStream(sk.getInputStream());
            out = new DataOutputStream(sk.getOutputStream());
            out.writeUTF("creargrupo " + nombreGrupo);
            String response = in.readUTF();

            if (response.equals("true")) {
                System.out.println("Grupo creado con éxito");
            } else {
                System.out.println("Error al crear el grupo.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    static class ClientConfiguration {
        public int tamanoMaximoArchivo;
        public String rutaDescargaArchivos;
        public String ipServidor;
        public int portServidor;

        public ClientConfiguration(int tamanoMaximoArchivo, String rutaDescargaArchivos, String ipServidor, int portServidor) {
            this.tamanoMaximoArchivo = tamanoMaximoArchivo;
            this.rutaDescargaArchivos = rutaDescargaArchivos;
            this.ipServidor = ipServidor;
            this.portServidor = portServidor;
        }
    }

    private static void loadClientConfiguration() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("client.properties")) {
            properties.load(fis);

            // Carga las variables del archivo de configuración
            clientConfig = new ClientConfiguration(
                    Integer.parseInt(properties.getProperty("tamanoMaximoArchivo")),
                    properties.getProperty("rutaDescargaArchivos"),
                    properties.getProperty("ipServidor"),
                    Integer.parseInt(properties.getProperty("portServidor"))
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
