//package progetto_laboratorio_di_reti;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

/**
 *
 * La classe WordleServerMain contiente il metodo main attraverso cui viene
 * mandato in esecuzione il server. Il metodo main legge i parametri di
 * configurazione del server dal relativo file di configurazione.
 * 
 * @author Andrea Lepori
 *
 */

public class WordleServerMain {

	// metodo main
	public static void main(String[] args) {

		// recupero i parametri di configurazione del server dal file di configurazione
		File config = new File(System.getProperty("user.dir"), "server_config.properties");
		FileInputStream fis = null;
		Properties prop = new Properties();
		try {
			fis = new FileInputStream(config);
			prop.load(fis);

			// start WordleServer
			WordleServer server = new WordleServer(prop.getProperty("multicast_address"),
					Integer.parseInt(prop.getProperty("multicast_port")),
					Integer.parseInt(prop.getProperty("registry_port")),
					Integer.parseInt(prop.getProperty("socket_tcp_port")),
					Integer.parseInt(prop.getProperty("time_to_awayt")),
					Integer.parseInt(prop.getProperty("registr_obj")),
					Integer.parseInt(prop.getProperty("notific_obj")),
					Integer.parseInt(prop.getProperty("time_to_refresh")));
			server.start();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}
		}
	}
}
