//package progetto_laboratorio_di_reti;

// IMPORT
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

// CODE 

/* 
 * La classe WordleClientMain contiente il metodo main attraverso cui viene mandato in esecuzione il client. 
 * La classe legge i parametri di configurazione del client dal relativo file di configurazione.
 */
public class WordleClientMain {

	/*
	 * metodo main client
	 */
	public static void main(String[] args) {

		// recupero i parametri di configurazione del client dal file di configurazione
		// user.dir : "In computing, the working directory of a process is a directory
		// of a
		// hierarchical file system, if any dynamically associated with each
		// process. It is sometimes called the current working directory (CWD)
		// i file di configurazione devono essere posti nella working directory
		File config = new File(System.getProperty("user.dir"), "client_config.properties");
		FileInputStream fis = null;
		Properties prop = new Properties();
		try {
			fis = new FileInputStream(config);
			prop.load(fis);

			// create WordleClient
			WordleClient client = new WordleClient(prop.getProperty("multicast_address"),
					Integer.parseInt(prop.getProperty("multicast_port")),
					Integer.parseInt(prop.getProperty("registry_port")),
					Integer.parseInt(prop.getProperty("socket_tcp_port")),
					Integer.parseInt(prop.getProperty("callback_obj")),
					Integer.parseInt(prop.getProperty("socket_tcp_timeout")));

			// start WordleClient
			client.start();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally { 
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}