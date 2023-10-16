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
    private static ClientConfiguration clientConfig;

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
            System.out.println("5. Ver archivos");
            System.out.println("6. Recibir archivo");
            System.out.println("7. Crear grupo");
            System.out.println("8. Gestionar grupo");
            System.out.println("9. Eliminar grupo");
            System.out.println("10. Salir");
            int opcion = verificarInput(sc);
            sc.nextLine();

            switch (opcion) {
                case 1:
                    listarUsuarios();
                    break;
                case 2:
                    enviarMensaje();
                    break;
                case 3:
                    listarMensaje();
                    break;
                case 4:
                    enviarArchivo();
                    break;
                case 5:
                    // verArchivos(); Método que falta, agrégalo
                    break;
                case 6:
                    recibirArchivo();
                    break;
                case 7:
                    crearGrupo();
                    break;
                case 8:
                    listarGrupos();
                    gestionarGrupo();
                    break;
                case 9:
                    listarGrupos();
                    eliminarGrupo();
                    break;
                case 10:
                    logout();
                    continuar = false;
                    break;
                default:
                    System.out.println("Opción inválida");
                    break;
            }
        }
    }

    public static boolean iniciarApp() {
        try {
            sk = new Socket(clientConfig.ipServidor, clientConfig.portServidor);
            out = new DataOutputStream(sk.getOutputStream());
            in = new DataInputStream(sk.getInputStream());

            System.out.println("1. Iniciar Sesión");
            System.out.println("2. Crear Cuenta");
            System.out.print("Elija una opción: ");
            int opcion = sc.nextInt();
            sc.nextLine();

            System.out.print("Usuario: ");
            String usuario = sc.nextLine();
            System.out.print("Contraseña: ");
            String password = sc.nextLine();



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
            System.out.println(response);
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

    public static void listarMensaje() {
        System.out.println("1. Mensaje de usuario");
        System.out.println("2. Mensaje de grupo");
        int opcion = verificarInput(sc);
        sc.nextLine();
        switch (opcion) {
            case 1:
                listarMensajesUsuario();
                break;
            case 2:
                listarMensajesGrupo();
                break;
            default:
                System.out.println("Opción inválida");
                break;
        }
    }

    public static void listarMensajesUsuario() {
        try {
            System.out.print("Recibir mensajes de: ");
            String destinatario = sc.nextLine();

            out.writeUTF("listarmensajes " + destinatario );

            String respuestaServidor = in.readUTF();
            System.out.println(respuestaServidor);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void listarMensajesGrupo() {
        try {
            System.out.print("Recibir mensajes de: ");
            String destinatario = sc.nextLine();

            out.writeUTF("listarmensajesgrupo " + destinatario );

            String respuestaServidor = in.readUTF();
            System.out.println(respuestaServidor);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void enviarArchivo() {
        try {
            System.out.print("Nombre del destinatario: ");
            String destinatario = sc.nextLine();
            System.out.print("Ruta del archivo: ");
            String rutaArchivo = sc.nextLine();
            File file = new File(rutaArchivo);
            if (file.length() > clientConfig.tamanoMaximoArchivo) {
                System.out.println("El archivo excede el tamaño máximo permitido");
                return;
            }
            out.writeUTF("enviararchivo " + destinatario + " " + rutaArchivo);
            FileInputStream fis = new FileInputStream(file);
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

    private static void crearGrupo() {
        try {
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("creargrupo " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Grupo creado con éxito");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Error al crear el grupo");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void listarGrupos() {
        try {
            out.writeUTF("listargrupos");
            String response = in.readUTF();
            if (response.equals("Comando incorrecto")) {
                System.out.println("Error al listar grupos");
            } else {
                String[] grupos = response.split(", ");
                for (String grupo : grupos) {
                    if (!grupo.equals("null")) {
                        System.out.println(grupo);
                    }
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void gestionarGrupo() {
        try {
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("vermiembros " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("Comando incorrecto")) {
                System.out.println("Error al listar miembros");
            } else {
                String[] miembros = response.split(", ");
                for (String miembro : miembros) {
                    if (!miembro.equals("null")) {
                        System.out.println(miembro);
                    }
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            boolean esAdmin = verificarSiEsAdmin(nombreGrupo);

            if (esAdmin) {
                System.out.println("1. Agregar usuario a un grupo");
                System.out.println("2. Eliminar usuario de un grupo");
                System.out.println("3. Dar/quitar permisos");
                int opcion = verificarInput(sc);
                sc.nextLine();
                switch (opcion) {
                    case 1:
                        agregarUsuarioGrupo();
                        break;
                    case 2:
                        eliminarUsuarioGrupo();
                        break;
                    case 3:
                        darQuitarPermisos();
                        break;
                    default:
                        System.out.println("Opción inválida");
                        break;
                }
            } else {
                System.out.println("No eres administrador del grupo");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean verificarSiEsAdmin(String nombreGrupo) {
        try {
            out.writeUTF("veradmin " + nombreGrupo);
            String response = in.readUTF();
            return response.equals("true");
        } catch (Exception e) {
            System.out.println("Error al verificar si es admin");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            return false;
        }
    }

    private static void agregarUsuarioGrupo() {
        try {
            System.out.print("Nombre del usuario: ");
            String nombreUsuario = sc.nextLine();
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("agregarusuario " + nombreUsuario + " " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Usuario agregado con éxito");
            } else {
                System.out.println("Error al agregar el usuario");
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void eliminarUsuarioGrupo() {
        try {
            System.out.print("Nombre del usuario: ");
            String nombreUsuario = sc.nextLine();
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("eliminarusuario " + nombreUsuario + " " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Usuario eliminado con éxito");
            } else {
                System.out.println("Error al eliminar el usuario");
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void darQuitarPermisos() {
        try {
            System.out.print("Nombre del usuario: ");
            String nombreUsuario = sc.nextLine();
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            System.out.println("1. Dar permisos");
            System.out.println("2. Quitar permisos");
            int opcion = verificarInput(sc);
            sc.nextLine();
            if (opcion == 1) {
                out.writeUTF("darpermisos " + nombreUsuario + " " + nombreGrupo);
            } else if (opcion == 2) {
                out.writeUTF("quitarpermisos " + nombreUsuario + " " + nombreGrupo);
            } else {
                System.out.println("Opción inválida");
            }
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Permisos modificados con éxito");
            } else {
                System.out.println("Error al modificar los permisos");
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void eliminarGrupo() {
        try {
            System.out.print("Nombre del grupo: ");
            String nombreGrupo = sc.nextLine();
            out.writeUTF("eliminargrupo " + nombreGrupo);
            String response = in.readUTF();
            if (response.equals("true")) {
                System.out.println("Grupo eliminado con éxito");
            } else {
                System.out.println(response);
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void logout() {
        try {
            out.writeUTF("logout");
            sk.close();
            System.out.println("Hasta luego!! ^^");
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
