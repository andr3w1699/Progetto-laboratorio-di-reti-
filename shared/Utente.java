//package progetto_laboratorio_di_reti;


/**
 * La classe Utente rappresenta un utente registrato.
 * <p>
 * Ogni utente è definito da uno username univoco (String), una password
 * (String, non vuota), un flag logged (boolean) che vale true se e solo se
 * l'utente è attualmente loggato, uno score (float) che rappresenta il
 * punteggio e un oggetto stats (della classe Statistiche) che rappresenta le
 * statistiche di interesse dell'utente. La classe Utente implementa
 * l'interfaccia Comparable perchè è definito un ordinamento sugli utenti in
 * base al punteggio.
 * 
 * @author Andrea Lepori
 * @version 1.0
 */

public class Utente implements Comparable<Utente> {

	// username dell'utente che è univoco
	private String username;

	// ogni utente ha una password che non può essere la stringa vuota
	private String password;

	// logged == true <--> utente loggato, logged == false <--> utente non loggato
	private boolean logged;

	// punteggio dell' utente
	private float score;

	// statistiche di interesse dell'utente
	private Statistiche stats;

	/**
	 * metodo costruttore
	 *
	 * @param username , stringa che identifica univocamente un utente, è necassario
	 *                 essere sicuri di passare al costruttore un username non già
	 *                 in uso da un altro utente nel database delle registrazioni
	 * 
	 * @param password , stringa non vuota che rappresenta la password dell' utente
	 * 
	 * @see Statistiche
	 * 
	 * @exception IllegalArgumentException , viene lanciata se la password è la
	 *                                     stringa vuota
	 * 
	 * @exception NullPointerException     , viene lanciata se username==null oppure
	 *                                     password==null
	 * 
	 */
	public Utente(String username, String password) throws IllegalArgumentException, NullPointerException {

		// controllo che nessuno dei due parametri sia null
		if (username == null || password == null)
			throw new NullPointerException();

		// controllo che la password non sia la stringa vuota
		if (password.equals(""))// altrimenti sollevo un'eccezione
			throw new IllegalArgumentException("La stringa vuota non è una password valida");

		// username e password sono quelli passati al costruttore
		this.username = username;
		this.password = password;

		// un utente quando viene registrato non è inizialmente loggato
		this.logged = false;

		// un utente quando viene registrato inizialmente ha punteggio 0
		this.score = (float) 0;

		// inizializzo le statistiche chiamando l'opportuno costruttore
		this.stats = new Statistiche();
	}

	// METODI GETTER
	// questi metodi sono sincronizzati attraverso l'utilizzo del modificatore
	// synchronized di Java per proteggere l'oggetto Utente da race condition
	// garantendo l'accesso in lettura e scrittura a this utente in mutua esclusione

	/**
	 * gets <code>this.password</code>
	 * 
	 * @return una stringa che rappresenta la password di this utente
	 */
	public synchronized String getPassword() {
		return this.password;
	}

	/**
	 * gets <code>this.username</code>
	 * 
	 * @return una stringa che rappresenta lo username di this utente
	 */
	public synchronized String getUsername() {
		return this.username;
	}

	/**
	 * gets <code>this.logged</code>
	 * 
	 * @return un boolean che vale true se e solo se this utente è attualmente
	 *         loggato, restituisce false altrimenti
	 */
	public synchronized boolean isLogged() {
		return this.logged;
	}

	/**
	 * gets <code>this.score</code>
	 * 
	 * @return un float che rappresenta il punteggio attuale di this utente
	 */
	public synchronized float getScore() {
		return this.score;
	}

	/**
	 * gets <code>this.stats</code>
	 * 
	 * @return un oggetto di tipo Statistiche che rappresenta le statistiche di this
	 *         utente
	 */
	public synchronized Statistiche getStatistiche() {
		return this.stats;
	}

	// METODI SETTER
	// questi metodi sono sincronizzati attraverso l'utilizzo del modificatore
	// synchronized di Java per proteggere l'oggetto Utente da race condition
	// garantendo l'accesso in lettura e scrittura a this utente in mutua esclusione

	/**
	 * sets <code>this.logged</code> to true
	 * 
	 */
	public synchronized void log() {
		this.logged = true;
	}

	/**
	 * sets <code>this.logged</code> to false
	 * 
	 */
	public synchronized void unlog() {
		this.logged = false;
	}

	/**
	 * setta <code>this.score</code> al valore passato in input
	 * 
	 * @param score , nuovo punteggio di this utente
	 */
	public synchronized void setScore(float score) {
		this.score = score;
	}

	// METODI DI SUPPORTO

	/**
	 * Metodo per il ricalcolo e l'aggiornamento del punteggio di this utente.
	 * <p>
	 * Il punteggio viene calcolato moltiplicando il numero di vittorie
	 * (num_vittorie) per un fattore di penalità (penalty_factor). Il fattore di
	 * penalità è uguale a (num_vittorie/num_tentativi) ed è pari al reciproco del
	 * numero medio di tentativi nelle partite vittoriose (1/(media_tentativi)). Più
	 * è grande il numero medio di tentativi per arrivare alla soluzione, più il
	 * fattore di penalty è piccolo e più penalizza il punteggio dell' utente che
	 * quindi è avvantaggiato dall' indovinare la secret word col minor numero di
	 * tentativi possibile.
	 * 
	 * @see Statistiche
	 */
	public synchronized void updateScore() {
		// numero di vittorie, chiamo l'opportuno metodo getter di Statistiche
		int num_vittorie = this.stats.getVittorie();

		// numero complessivo dei tentativi impiegati nelle partite vittoriose
		// chiamo l'opportuno metodo getter di Statistiche
		int num_tentativi = this.stats.getTentativi();

		if (num_vittorie != 0) { // ho vinto almeno una partita, posso fare tranquillamente la divisione di cui
									// sotto, senza correre il rischio dividere per 0
			// numero medio dei tentavi impiegati nelle vittorie
			float media_tentativi = (float) num_tentativi / (float) num_vittorie;
			// fattore di penalità calcolato come il reciproco della media_tentativi
			// media_tentativi alta --> penalty_factor piccolo (tendente a 0) --> grande
			// penalità sul punteggio
			// media_tentativi bassa --> penalty_factor tendente a 1 --> bassa penalità sul
			// punteggio
			float penalty_factor = (float) 1 / media_tentativi;
			// chiamo la setScore col punteggio appena calcolato
			this.setScore(num_vittorie * penalty_factor);
		} else
			// non ho vinto partite --> punteggio 0
			this.setScore((float) 0);

	}

	// @override : riscrivo il metodo equals per la classe Utente
	public  boolean equals(Object obj) {
		// controllo se l'oggetto passato in input obj è un'istanza della classe Utente
		if (obj instanceof Utente)
			// obj è un'istanza di utente, controllo l'uguaglianza degli username,
			// che sappiamo individuare univocamente un Utente,
			// utilizzando il metodo equals che è definito per le stringhe
			return ((Utente) obj).getUsername().equals(this.username);
		// se non entro nell' if significa che obj non è un oggetto della classe Utente
		// restituisco perciò false
		return false;
	}

	// @override : riscrivo il metodo toString per la classe Utente
	public  String toString() {

		return "username :" + this.username + "\n" + "password :" + "*".repeat(this.password.length()) + "\n" + "logged :" + this.logged
				+ "\n" + "score :" + this.score + "\n" + "statistiche :" + this.stats.toString() + "\n";

	}

	/**
	 * L'ordinamento degli utenti è definito in base al punteggio. A parità di
	 * punteggio vale l'ordinamento lessicografico degli username degli utenti.
	 * 
	 * @override : riscrivo il metodo compareTo per la classe Utente
	 * 
	 * @param u Utente to compare with this Utente
	 * 
	 * @return a int value : 0 if param u is equal to the this utente, < 0 if the
	 *         score of param u is smaller than the score of this utente, > 0 if the
	 *         score of param u is bigger than the score of this utente.
	 * 
	 * @exception NullPointerException if u == null
	 */

	public  int compareTo(Utente u) throws NullPointerException {
		// controllo che u non sia null
		if (u == null)
			throw new NullPointerException();

		// se due utenti hanno lo stesso username sono lo stesso utente
		if (this.username.equals(u.getUsername()))
			return 0;

		// confronto i due punteggi
		float result = u.getScore() - this.score;
		if (result < 0)
			return -1;
		else if (result > 0)
			return 1;
		// se i due punteggi sono uguali, confronto gli username dei due utenti
		// ovvero a parità di punteggio ordino lessicograficamente
		else
			// the value 0 if the argument string is equal to this string; a value less than
			// 0 if this string is lexicographically less than the string argument; and
			// a value greater than 0 if this string is lexicographically greater than the
			// string argument.
			return this.username.compareTo(u.getUsername());

	}

}
