package servidorTCP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.nio.file.Files;

public class ThreadConexion extends Thread {

	public final static int MESSAGE_SIZE = 1024;

	public String archivo;

	private Socket socket = null;

	private BufferedWriter writer;

	private int cliente;

	public ThreadConexion(Socket pSocket, String arch, BufferedWriter writer, int cliente) {
		socket = pSocket;
		this.writer = writer;
		this.cliente = cliente;
		archivo = arch;
		try {
			socket.setSoTimeout(30000);
		} catch (SocketException e) {
			System.out.println("Hubo un error con el envio del archivo "+ e.getMessage());
			try {

				writer.write("Hubo un error con el envio del archivo "+ e.getMessage());
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				System.out.println("Hubo un error escribiendo el error "+ ex.getMessage());
			}
		}

	}

	public void run() {

		try {
			enviar();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				System.out.println("Error enviando el archivo "+ e.getMessage());
				writer.write("Hubo un error con el envio del archivo");
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				System.out.println("Error escribiendo el error "+ ex.getMessage());
			}
		}

	}

	public void enviar() {

		FileInputStream fis = null;
		BufferedInputStream bis = null;

		OutputStream os = null;

		try {
			os = new BufferedOutputStream(socket.getOutputStream());
			File myFile = new File(archivo);
			byte[] myByteArray = Files.readAllBytes(myFile.toPath());

			fis = new FileInputStream(myFile);
			bis = new BufferedInputStream(fis);

			byte[] hash = new byte[61440];
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			hash = md.digest(myByteArray);
			StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < hash.length; i++) {
	            sb.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
	        }
	        
	        String hashEnviar = sb.toString();
			DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
			dOut.writeUTF(hashEnviar);

			bis.read(myByteArray, 0, myByteArray.length);

			System.out.println("Enviando (" + myByteArray.length + " bytes)");

			int enviados = 0;
			int paquetes = 0;
			while (enviados < myByteArray.length) {
				paquetes++;
				if (enviados + MESSAGE_SIZE < myByteArray.length) {
					os.write(myByteArray, enviados, MESSAGE_SIZE);
					enviados += MESSAGE_SIZE;
				} else {
					int restantes = myByteArray.length - enviados;
					os.write(myByteArray, enviados, restantes);
					enviados += restantes + 1;
				}
			}

			String cadena = "Se enviaron " + paquetes + " paquetes al cliente " + cliente + " para un total de " + paquetes*256 + " bytes.";
			writer.write(cadena);
			writer.newLine();
			writer.flush();

			writer.write("Cada paquete con un tamanio de " + MESSAGE_SIZE + " bytes");
			writer.newLine();
			writer.flush();

			os.flush();
			System.out.println("Archivo enviado al cliente "+ cliente );
			writer.write("Archivo enviado exitosamente al cliente " + cliente);
			writer.newLine();
			writer.flush();

			writer.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			try {
				System.out.println("Error enviando el archivo "+ e.getMessage());
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				System.out.println("Error escribiendo el error "+ ex.getMessage());
			}
		}
	}
}
