import javafx.application.Application;
import java.util.ArrayList;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafx.stage.Stage;

public class ATMTest extends Application{
	ATMInterface model =  ATMFactory.getInstance();
	ATMControllerInterface controller = new ATMController(model);
	public static void main(String[] args){
		launch(args);
	}
	
	public void start(Stage primaryStage){
		ATMView atmview = controller.getView();
		atmview.createView();
		primaryStage.setScene(atmview.scene);
		Stage dialstage = new Stage();
		dialstage.setScene(atmview.dialpad.DialScene);
		dialstage.setTitle("ATM Dialpad");
		primaryStage.setResizable(false);
		primaryStage.sizeToScene();
		dialstage.setResizable(false);
		dialstage.sizeToScene();
		primaryStage.setTitle("ATM");
		dialstage.show();
		primaryStage.show();
	}
}

/*
 * ATM Project:
 * Use multiple patterns to design a meaningful application.
 * Gain experience in a simulated real world design process with a team.
 * 
 * Group members:
 * Group Leader: Jesse Minton - Designed our MVC pattern, major gui improvements and optimizations, guided the team to design and build a blackbox ATM using MVC pattern.
 * Taivon Watkins - Aided in design of our MVC pattern, as well as designed the ATMFactory class blackbox.
 * Nicole Christian- Designed the BankCompany factories for use in a black box.
 * Charles Curtois - Wrote use cases for project, as well as helped design the ATMFactory class blackbox.
 * Dylan Myers - Laid the groundwork for our GUI, designed the initial stages of all GUI work for the MVC pattern.
 * Walter Ivy - Wrote our command pattern designs for use in our ATMFactory/Controller.
 * 
 * All members:
 * Met and discussed project as a team.
 * Aided in programming, thoughts, suggestions, or bug-fixing.
 * 
 */


//Our ATMInteface which designates all the methods needed for our factory.
interface ATMInterface{
	//Method to establish the BankCompany Factory class to a variable.
	boolean getBank()throws InstantiationException,
	IllegalAccessException, ClassNotFoundException;
	//Method to get the current balance of the ATM.
	double getBalance();
	//Method to get the state.
	int getState();
	//Method to get the withdraw limit of the ATM (limit user withdraw amount).
	int getLimit();
	//Method to deposit to the BankCompany Factory class.
	boolean deposit(int x);
	//Method for card swiping.
	boolean cardSwiped(int x);
	//Method for BankCompany Factory class withdraw.
	boolean withdraw(int x);
	//Method to see if the current user is a member.
	boolean member();
	//Method to set the BankCompany Factory class.
	void setBank(String b);
	//Method for allowing extra transactions.
	void anotherTransaction();
	//Method for setting and checking pin.
	void setPin(int x);
	//Method to cancel our current path and return.
	void cancel();
	//Method for adding/removing observers.
	void registerObserver(StateObserver o);
	void removeObserver(StateObserver o);
	//Method for updating based on user choice in state 2.
	void update(int x);
	//Method for printing and cleaning receipt.
	void printreceipt(boolean print);
	//Method for wiping and starting over.
	void wipe()throws InstantiationException, 
	IllegalAccessException, ClassNotFoundException;
}

class ATMFactory implements ATMInterface{
	//Our variables:
	//The model.
	static ATMFactory atm;
	//List of StateObservers. Typically one but arraylist for modularity.
	ArrayList<StateObserver> observers = new ArrayList<StateObserver>();
	//BankCompany factory class variable.
	BankCompany bank;
	//Balance of the ATM initially is 1000.
	private double balance = 1000;
	//The ATM's limit per transaction
	private int limit;
	//The ATM's state.
	private int state = 0;
	ReceiptPrinter atmreceiptprinter = new ReceiptPrinter();
	Dispenser atmdispenser = new Dispenser();
	DispenseCommand dispense = new DispenseCommand(atmdispenser);
	PrintReceiptCommand printreceipt = new PrintReceiptCommand(atmreceiptprinter);
	//Receipt
	private String receipt = "---Suntrust---";
	private boolean ismember = false;
	private int cardnumber = 0;
	
	//The anotherTransaction method is only usable in state 9, and simply means the user chose another transaction. Instead of wiping, go back to the menu.
	public void anotherTransaction(){
		if(state==9){
			state=2;
			notifyStateObservers();
		}
	}
	//When we wipe we reset the variables to ensure safety.
	public void wipe() throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		bank = (BankCompany)Class.forName("NullBank").newInstance();
		cardnumber = 0;
		ismember = false;
	}
	//Printing the receipt
	public void printreceipt(boolean print){
		if(print){
			printreceipt.execute(receipt);
		}
		receipt = "---Suntrust---";
		state=9;
		notifyStateObservers();
	}
	//Method for observers to get the state.
	public int getState(){
		return state;
	}
	//Register/removing observers.
	public void registerObserver(StateObserver o){
		observers.add(o);
	}
	public void removeObserver(StateObserver o){
		observers.remove(o);
	}
	//Constructor is private for singleton.
	private ATMFactory() {
		System.setProperty("bankFrom", "NullBank");
	}
	
	//Bank will be set on a card number pseudo-algorithm
	public void setBank(String b) {
		System.setProperty("bankFrom", "From" + b );
	}
	//Here we will assign the bank account information to the ATM.
	//To avoid repetition/confusing details we will store this information.
	public boolean getBank() throws InstantiationException,
	IllegalAccessException, ClassNotFoundException {
			String s  = System.getProperty("bankFrom");
			bank = (BankCompany)Class.forName(s).newInstance();
			return true;
	}
	//Method to check membership, if member, deposit and no surcharge activated.
	public boolean member(){
		return ismember;
	}
	//Singleton method for instantiation.
	public synchronized static ATMFactory getInstance() {
		if(atm == null) {
			atm = new ATMFactory();
		}
		return atm;
	}
	//Method for card swiping.
	public boolean cardSwiped(int cardnumber){
		if(state == 0){
			int cardthousand = cardnumber/100000;
			if(cardthousand == 1){
				System.setProperty("bankFrom", "FromBBT");
				state = 1;
			}
			else if(cardthousand == 2){
				System.setProperty("bankFrom", "fromSuntrust");
				ismember = true;
				state = 1;
			}
			else if(cardthousand == 3){
			System.setProperty("bankFrom", "fromBankOfAmerica");	
				state = 1;
			}
			else{
				System.out.println("Invalid Card");
				cardnumber = 0;
			}
			boolean hold = false;
			if(cardnumber != 0){
				this.cardnumber = cardnumber;
				try {
					if(getBank()) hold = true;
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			notifyStateObservers();
			return hold;
		}
		return false;
	}
	//Method for notifying state observers and updating view based on state.
	public void notifyStateObservers(){
		for(int i=0; i<observers.size(); i++){
			StateObserver observer = (StateObserver)observers.get(i);
			observer.update();
		}
	}
	//Method for depositing into the BankCompany Factory class assigned.
	public boolean deposit(int x){
		if(state==5){
			state = 7;
		}
		else if(state==7){
			bank.Deposit(x);
			receipt = receipt +"\n Deposit: "+x;
			state = 8;
		}
		else{
			return false;
		}
		notifyStateObservers();
		return true;
	}
	//The Cancel method will revert back to the menu state, however if you're at the pin page, menu, or transact again page, it will go back to 0 and wipe.
	public void cancel(){
		if(state==1 || state==2 || state==9){
				try {
					wipe();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			state=0;
		}
		else if(state==3 || state==4 || state==5 || state==6 || state==7 || state==8){
			state=2;
		}
		notifyStateObservers();
	}
	//The update method is really more of a single choice method, we receive the menu response designating which choice the user made.
	public void update(int x){
		if(state==2){
			if(x==1){
				state = 3;
			}
			else if(x==2){
				state = 4;
			}
			else if(x==3){
				state = 5;
			}
		}
		notifyStateObservers();
	}
	//Withdraw method for withdrawing from the BankCompany Factory class.
	public boolean withdraw(int x){
		if(x%20 != 0){
			return false;
		}
		else if(state==4){
			state=6;
		}
		else if(state==6){
			if(bank.Withdraw(x+3)){
				dispense.execute(x);
				state=8;
				receipt = receipt+"\n Withdraw: "+(x+3);
			}
			else{
				state=4;
				notifyStateObservers();
				return false;
			}
		}
		notifyStateObservers();
		return true;
	}
	//Method for pin setting.
	public void setPin(int pin) {
		//Checking pin is 4-digits as required.
		if(pin%10000 < 1000 ){
			System.out.println("ERROR: 4 DIGIT PIN ONLY");
		}
		else{
		//Verifying our pin, otherwise don't change state, pin wrong.
		if(bank.verify(pin, cardnumber)){
			state = 2;
			notifyStateObservers();
		}
		}
	}
	//Method for getting the balance.
	public double getBalance() {
		state = 3;
		notifyStateObservers();
		return bank.getBalance();
	}
	//Not to be confused with getBalance, this setBalance sets the ATM's current balance, admins only.
	public void setBalance(int balance) {
		this.balance = balance;
	}
	//Get the current withdraw per user limit.
	public int getLimit() {
		return limit;
	}
	//Set the current withdraw per user limit.
	public void setLimit(int limit) {
		this.limit = limit;
	}
}

class ATMView implements StateObserver{
	//Our View receives data from a model.
		ATMInterface model;
	//Our view sends data through a controller.
		ATMControllerInterface controller;
		private int state;
		private int numOfAttempts;
	//Extra bit of GUI to simulate a physical dialpad.	
		DialPad dialpad = new DialPad();
	//Styling panes, the background view is static.
		BorderPane bpane;
		StackPane spane;
		Rectangle r;
		Rectangle r2;
		Rectangle r3;
		Rectangle r4;
		Text atmlabel;	
		Scene scene;
//All these are the variables chunked together into which state they belong to.
		BorderPane[] panes;
		Text carderror;
		Text cardlabel;
		
		Alert alert = new Alert(AlertType.ERROR);
		Label pinerror;
		PasswordField pinbox;
		Text pinlabel;
		VBox pincontainer;		
		
		Text menulabel, menumenu;
		VBox menuVbox;
		HBox menuHbox;

		Label withdrawerror;
		Text withdrawlabel, withdrawcancellabel;
		int withdrawamountholder = 0;
		TextField withdrawamount;
		VBox withdrawcontainer;
		
		Text balancelabel, returnlabel;
		VBox balancebox;		
		
		VBox depositcontainer;
		TextField depositamount;
		Text depositlabel, depositcancellabel;
		
		Text withdrawconfirmlabel;
		Text withdrawconfirmmenu;
		VBox withdrawconfirmcontainer;
		
		Text depositconfirmlabel;
		int depositamountholder = 0;
		Text depositconfirmmenu;
		VBox depositconfirmcontainer;
		
		Text receiptlabel;
		Text receiptmenu;
		VBox receiptcontainer;
		
		Text transactagainlabel;
		Text transactagainmenu;
		VBox transactagaincontainer;
		
		FadeTransition pinFade = fadeCreate(pinerror);
		FadeTransition withdrawFade = fadeCreate(withdrawerror);
		FadeTransition cardFade = fadeCreate(carderror);
		
		private FadeTransition fadeCreate(Node node) {
		        FadeTransition fade = new FadeTransition(Duration.seconds(3), node);
		        //Setting the opacity value to 1 to display the fade object
		        fade.setFromValue(1);
		        //Fade object disappears
		        fade.setToValue(0);
		        
		        return fade;
		 }
		//Creating a view requires the controller, and the model objects.
		ATMView(ATMControllerInterface controller, ATMInterface model){
			this.controller = controller;
			this.model = model;		
			model.registerObserver((StateObserver) this);
		}
		//Method to create our view and style.
		public void createView(){
			//Building our pad.
			dialpad.buildPad();
			//Setting the actions for the buttons on the dialpad which react based on the state.
			//Names are self-explanatory. numeric1 = a numeric integer 1 button.
			dialpad.numeric1.setOnAction(e -> {
				//If state==1 we want the pin, update pinbox.
				if(state==1){
					if(pinbox.getText().length() < 4){
						pinbox.setText(pinbox.getText()+1);
					}
				}
				//if state==2 we want the user response.
				else if(state==2){
					balancelabel.setText(balancelabel.getText() +controller.getBalance());	
				}
				//4, we want a withdraw amount.
				else if(state==4){
					if(withdrawamount.getText().length() < 4){
						withdrawamount.setText(withdrawamount.getText() +1);
					}
				}
			});
			dialpad.numeric2.setOnAction(e -> {
				//1 change pin.
				if(state==1){
					if(pinbox.getText().length() < 4){
						pinbox.setText(pinbox.getText()+2);
					}
				}
				//2 we want a choice.
				else if(state==2){
					controller.update(2);
				}
				//4 we want an amount.
				else if(state==4){
					if(withdrawamount.getText().length() < 4){
						withdrawamount.setText(withdrawamount.getText() +2);
					}
				}
			});
			dialpad.numeric3.setOnAction(e -> {
				//1...
				if(state==1){
					if(pinbox.getText().length() < 4){
						pinbox.setText(pinbox.getText()+3);
					}
				}
				//2...
				else if(state==2){
					controller.update(3);
				}
				//4...
				else if(state==4){
					if(withdrawamount.getText().length() < 4){
						withdrawamount.setText(withdrawamount.getText() +3);
					}
				}

			});
			//Pattern continues for all numerics, however 1-3 are special numerics that will be used by the model to send in a choice at state 2, so 4-0 lack this feature.
			dialpad.numeric4.setOnAction(e -> {

				if(state==1){
					if(pinbox.getText().length() < 4){
						pinbox.setText(pinbox.getText()+4);
					}
				}
				else if(state==4){
					if(withdrawamount.getText().length() < 4){
						withdrawamount.setText(withdrawamount.getText() +4);
					}
				}
			});
			dialpad.numeric5.setOnAction(e -> {

				if(state==1){
					if(pinbox.getText().length() < 4){
						pinbox.setText(pinbox.getText()+5);
					}
				}
				else if(state==4){
					if(withdrawamount.getText().length() < 4){
						withdrawamount.setText(withdrawamount.getText() +5);
					}
				}

			});
			dialpad.numeric6.setOnAction(e -> {
				if(state==1){
					if(pinbox.getText().length() < 4){
						pinbox.setText(pinbox.getText()+6);
					}
				}
				else if(state==4){
					if(withdrawamount.getText().length() < 4){
						withdrawamount.setText(withdrawamount.getText() +6);
					}
				}
			});
			dialpad.numeric7.setOnAction(e -> {

				if(state==1){
					if(pinbox.getText().length() < 4){
						pinbox.setText(pinbox.getText()+7);
					}
				}
				else if(state==4){
					if(withdrawamount.getText().length() < 4){
						withdrawamount.setText(withdrawamount.getText() +7);
					}
				}
			});
			dialpad.numeric8.setOnAction(e -> {

				if(state==1){
					if(pinbox.getText().length() < 4){
						pinbox.setText(pinbox.getText()+8);
					}
				}
				else if(state==4){
					if(withdrawamount.getText().length() < 4){
						withdrawamount.setText(withdrawamount.getText() +8);
					}
				}
			});
			dialpad.numeric9.setOnAction(e -> {

				if(state==1){
					if(pinbox.getText().length() < 4){
						pinbox.setText(pinbox.getText()+9);
					}
				}
				else if(state==4){
					if(withdrawamount.getText().length() < 4){
						withdrawamount.setText(withdrawamount.getText() +9);
					}
				}
			});

			
			dialpad.numeric0.setOnAction(e -> {
				if(state==1){
					if(pinbox.getText().length() < 4){
						pinbox.setText(pinbox.getText()+0);
					}
				}
				else if(state==4){
					if(withdrawamount.getText().length() < 4){
						withdrawamount.setText(withdrawamount.getText() +0);
					}
				}
			});
			//For the non-numeric buttons.
			dialpad.confirm.setOnAction(e -> {
				//Confirm that we have a 4 length pin and then try to pass it along.
				if(state==1){
					if(pinbox.getText().length() == 4){
						controller.setPin(Integer.parseInt(pinbox.getText()));
						pinbox.setText("");
						//Error fades after 
						 SequentialTransition fade = new SequentialTransition(pinerror,pinFade);
						 fade.play();
							numOfAttempts++;
						//After three attempts the user's card will be eaten
							if(numOfAttempts == 3 && state == 1){
								controller.eatcard();
								alert.show();
								pinerror.setOpacity(0);
								controller.cancel();
								if(state == 0){
									numOfAttempts = 0;
								}
								try {
									Thread.sleep(3000);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
								alert.close();
							}
					}
				}
				//Confirm we are finished inputting.
				else if(state==4){
					withdrawamountholder = Integer.parseInt(withdrawamount.getText());
					//Making sure user inputs amount in multiples of 20
					if(withdrawamountholder%20 != 0){
						//Insert error.
						withdrawerror.setText("Please enter in multiples of 20");
						//Fading the label after 3 seconds
						 SequentialTransition fade = new SequentialTransition(withdrawerror,withdrawFade);
						 fade.play();
						withdrawamountholder = 0;
					}
					//Error if amount exceeds balance
				
					else{
						if(controller.withdraw(withdrawamountholder)){
							withdrawconfirmlabel.setText(withdrawconfirmlabel.getText()+"\n" +withdrawamountholder+".00\n\n");
							if(!controller.member()){
								withdrawconfirmlabel.setText(withdrawconfirmlabel.getText() +"Also, a 3$ surcharge \nwill be added to\n your bill.\n");
							}
						}
						else{
								withdrawamountholder = 0;
						}
					}
					withdrawamount.clear();
				}
				//Confirm withdraw amount.
				else if(state==6){
					if(!controller.withdraw(withdrawamountholder)){
						if(withdrawerror.getText() != "Not enough funds to withdraw"){
							withdrawerror.setText("Not enough funds to withdraw");
						}
							 SequentialTransition fade = new SequentialTransition(withdrawerror,withdrawFade);
							 fade.play();
							withdrawamountholder = 0;
					}
					withdrawconfirmlabel.setText("PLEASE CONFIRM AMOUNT: ");
				}
				//Confirm deposit amount.
				else if(state==7){
					controller.deposit(depositamountholder);
					depositconfirmmenu.setText("PLEASE CONFIRM DEPOSIT: ");
				}
				//User wants to print receipt.
				else if(state==8){
					controller.printreceipt(true);
				}
				//User wants another transaction.
				else if(state==9){
					controller.anotherTransaction();
				}
			});
			dialpad.cancel.setOnAction(e -> {
				//User wants to cancel total transaction.
				if(state==1){
					pinbox.clear();
					menumenu.setText("1. BALANCE\n\n2. WITHDRAWAL\n\n");
					controller.cancel();
						numOfAttempts = 0;
					
				}
				//User wants to cancel total transaction.
				else if(state==2){
					menumenu.setText("1. BALANCE\n\n2. WITHDRAWAL\n\n");
					controller.cancel();
						numOfAttempts = 0;
					
				}
				//User wants to cancel the view balance option.
				else if(state==3){
					balancelabel.setText("AVAILABLE BALANCE: ");
					controller.cancel();
				}
				//User wants to cancel ...
				else if(state==4){
					withdrawamount.clear();
					controller.cancel();
						
				}
				else if(state==5){
					controller.cancel();
				
				}
				else if(state==6){
					withdrawconfirmlabel.setText("PLEASE CONFIRM AMOUNT:");
					controller.cancel();
					
				}
				else if(state==7){
					depositconfirmlabel.setText("PLEASE CONFIRM DEPOSIT: ");
					controller.rebukeDeposit();
					controller.cancel();
					
				}
				else if(state==8){
					controller.printreceipt(false);
					
				}
				//User wants to cancel total transaction.
				else if(state==9){
					menumenu.setText("1. BALANCE\n\n2. WITHDRAWAL\n\n");
					controller.cancel();
					numOfAttempts = 0;
				}
			});
			//Button for ease clearing TextFields (pin and withdraw).
			dialpad.clear.setOnAction(e -> {
				if(state==1){
						pinbox.clear();
				}
				else if(state==4){
					withdrawamount.clear();
				}
			});
			dialpad.CardDemo.setOnAction(e -> {
				//Our card demo has been rigged to fail to show the error text here, but will give you the correct pin on the second press for a non-member, then the third will be a member, and the third a different non-member.
				if(state==0){
					if(dialpad.padinteger == 0){
						dialpad.padinteger++;
						if(!controller.cardSwiped(400000)){
							SequentialTransition fade = new SequentialTransition(carderror, cardFade);
							fade.play();
						}
					}
					else if(dialpad.padinteger == 1){
						dialpad.padinteger++;
						//If the card swiped is not true, then show error.
						if(!controller.cardSwiped(100000)){
							SequentialTransition fade = new SequentialTransition(carderror, cardFade);
							fade.play();
						}
					}
					else if(dialpad.padinteger == 2){
						dialpad.padinteger++;
						if(!controller.cardSwiped(200000)){
							SequentialTransition fade = new SequentialTransition(carderror, cardFade);
							fade.play();
						}
					}
					else{
						if(!controller.cardSwiped(300000)){
							SequentialTransition fade = new SequentialTransition(carderror, cardFade);
							fade.play();
						}
					}
					if(state==1){
						if(controller.member()){
							menumenu.setText(menumenu.getText() +"3. DEPOSIT");
						}
					}
				}
			});
			//Button to help simulate a deposit in state 5.
			dialpad.Deposit100.setOnAction(e -> {
				if(state==5){
					if(depositconfirmlabel.getText() != "PLEASE CONFIRM DEPOSIT: ") depositconfirmlabel.setText("PLEASE CONFIRM DEPOSIT: ");
						depositconfirmlabel.setText(depositconfirmlabel.getText()+"\n\n$" +100+".00\n");
						controller.deposit(100);
						depositamountholder = 100;
				}
			});
			//Here we initialize and style all view elements.
			bpane = new BorderPane();
			spane = new StackPane();
			r = new Rectangle(600, 70);
			r2 = new Rectangle(600, 70);
			r3 = new Rectangle(70, 460);
			r4 = new Rectangle(70, 460);
			atmlabel = new Text("Suntrust");	
			scene = new Scene(bpane, 600, 600);
			
			bpane.setTop(spane);
			bpane.setBottom(r2);
			bpane.setLeft(r3);
			bpane.setRight(r4);
			bpane.setStyle("-fx-background-color: lightblue");
			atmlabel.setFill(Color.WHITE);
			atmlabel.setFont(Font.font("Arial Black", 50));
			r.setFill(Color.DARKBLUE);
			r2.setFill(Color.DARKBLUE);
			r3.setFill(Color.DARKBLUE);
			r4.setFill(Color.DARKBLUE);
			spane.getChildren().add(r);
			spane.getChildren().add(atmlabel);	
			
			panes = new BorderPane[11];
			for(int i=0; i<panes.length; i++){
				panes[i] = new BorderPane();
			}
			
			//Card error text
			carderror = new Text("ERROR: PROCESSING CARD");
			cardlabel = new Text("PLEASE INSERT CARD");
			pinbox = new PasswordField();
			
			alert.setTitle("Error Dialog");
			alert.setHeaderText(null);
			alert.setContentText("Your card has been eaten");
			
			carderror.setFont(Font.font("Arial Black", 15));
			carderror.setFill(Color.RED);
			carderror.setOpacity(0);
			cardlabel.setFont(Font.font("Arial Black", 35));
			cardlabel.setFill(Color.WHITE);
			panes[0].setAlignment(carderror, Pos.TOP_CENTER);
			panes[0].setTop(carderror);
			panes[0].setCenter(cardlabel);
			
			pinerror = new Label("ERROR: INCORRECT PIN");
			pinlabel = new Text("PLEASE INPUT PIN:");
			pincontainer = new VBox();	
			
			
			pinerror.setFont(Font.font("Arial Black", 15));
			pinerror.setTextFill(Color.RED);
			pinerror.setOpacity(0);
			pinlabel.setFont(Font.font("Arial Black", 30));
			pinlabel.setFill(Color.WHITE);
			pincontainer.getChildren().add(pinlabel);
			pincontainer.getChildren().add(pinbox);
			pincontainer.setAlignment(Pos.CENTER);
			pincontainer.setSpacing(30);
			pinbox.setMaxWidth(100);
			pinbox.setStyle("-fx-padding: 0 10 0 10");
			pinbox.setFont(Font.font("Arial Black", 30));
			panes[1].setAlignment(pinerror, Pos.TOP_CENTER);
			panes[1].setTop(pinerror);
			panes[1].setCenter(pincontainer);
			
			menulabel = new Text("PLEASE CHOOSE AN OPTION: ");			
			menumenu = new Text("1. BALANCE\n\n2. WITHDRAWAL\n\n");
			menuVbox = new VBox();
			menuHbox = new HBox();
			
			menulabel.setFont(Font.font("Arial Black", 26));
			menulabel.setFill(Color.WHITE);
			menumenu.setFont(Font.font("Arial Black", 20));
			menumenu.setFill(Color.WHITE);
			menumenu.setTextAlignment(TextAlignment.CENTER);
			menuVbox.setAlignment(Pos.CENTER);
			menuVbox.getChildren().add(menulabel);
			menuVbox.getChildren().add(menumenu);
			menuVbox.setSpacing(30);			
			panes[2].setCenter(menuVbox);

			returnlabel = new Text("\nCANCEL");			
			balancelabel = new Text("AVAILABLE BALANCE: ");
			balancebox = new VBox();
			
			balancelabel.setFont(Font.font("Arial Black", 26));
			balancelabel.setFill(Color.WHITE);
			returnlabel.setFont(Font.font("Arial Black", 18));
			returnlabel.setFill(Color.WHITE);
			balancebox.getChildren().add(balancelabel);
			balancebox.getChildren().add(returnlabel);
			balancebox.setAlignment(Pos.CENTER);
			panes[3].setCenter(balancebox);

			withdrawerror = new Label("Not enough funds to withdraw");
			withdrawlabel = new Text("ENTER WITHDRAWAL AMOUNT:");
			withdrawcancellabel = new Text("CONFIRM\n\nCANCEL\n\nCLEAR");
			withdrawcancellabel.setTextAlignment(TextAlignment.CENTER);
			withdrawamount = new TextField();
			withdrawcontainer = new VBox();	
			
			withdrawerror.setFont(Font.font("Arial Black", 20));
			withdrawerror.setTextFill(Color.RED);
			withdrawerror.setOpacity(0);
			withdrawlabel.setFont(Font.font("Arial Black", 25));
			withdrawlabel.setFill(Color.WHITE);
			withdrawcancellabel.setFont(Font.font("Arial Black", 15));
			withdrawcancellabel.setFill(Color.WHITE);
			withdrawcontainer.getChildren().add(withdrawlabel);
			withdrawcontainer.getChildren().add(withdrawamount);
			withdrawcontainer.getChildren().add(withdrawcancellabel);
			withdrawcontainer.setAlignment(Pos.CENTER);
			withdrawcontainer.setSpacing(30);
			withdrawamount.setAlignment(Pos.CENTER_RIGHT);
			withdrawamount.setFont(Font.font("Arial Black", 20));
			withdrawamount.setMaxWidth(75);
			withdrawamount.setMinHeight(30);
			withdrawamount.setMaxHeight(30);
			withdrawamount.setPadding(new Insets(0, 10, 0, 10));
			panes[4].setAlignment(withdrawerror, Pos.TOP_CENTER);
			panes[4].setTop(withdrawerror);
			panes[4].setCenter(withdrawcontainer);
			
			depositcontainer = new VBox();
			depositlabel = new Text("PLEASE INPUT YOUR DEPOSIT NOW.");
			depositcancellabel = new Text("CANCEL");
			
			depositlabel.setFont(Font.font("Arial Black", 20));
			depositlabel.setFill(Color.WHITE);
			depositcancellabel.setFont(Font.font("Arial Black", 18));
			depositcancellabel.setFill(Color.WHITE);
			depositcontainer.getChildren().add(depositlabel);
			depositcontainer.getChildren().add(depositcancellabel);
			depositcontainer.setSpacing(30);
			depositcontainer.setAlignment(Pos.CENTER);
			panes[5].setCenter(depositcontainer);
			
			
			withdrawconfirmlabel = new Text("PLEASE CONFIRM AMOUNT:\n");
			withdrawconfirmmenu = new Text("CONFIRM\n\nCANCEL");
			withdrawconfirmcontainer = new VBox();
			
			withdrawconfirmlabel.setFont(Font.font("Arial Black", 25));
			withdrawconfirmlabel.setFill(Color.WHITE);
			withdrawconfirmlabel.setTextAlignment(TextAlignment.CENTER);
			withdrawconfirmmenu.setTextAlignment(TextAlignment.CENTER);
			withdrawconfirmmenu.setFont(Font.font("Arial Black", 15));
			withdrawconfirmmenu.setFill(Color.WHITE);
			withdrawconfirmcontainer.getChildren().add(withdrawconfirmlabel);
			withdrawconfirmcontainer.getChildren().add(withdrawconfirmmenu);
			withdrawconfirmcontainer.setAlignment(Pos.CENTER);
			panes[6].setCenter(withdrawconfirmcontainer);
			
			depositconfirmlabel = new Text("PLEASE CONFIRM DEPOSIT: ");
			depositconfirmmenu = new Text("CONFIRM\n\nCANCEL");
			depositconfirmcontainer = new VBox();
			
			depositconfirmlabel.setFont(Font.font("Arial Black", 25));
			depositconfirmlabel.setFill(Color.WHITE);
			depositconfirmlabel.setTextAlignment(TextAlignment.CENTER);
			depositconfirmmenu.setFont(Font.font("Arial Black", 15));
			depositconfirmmenu.setFill(Color.WHITE);
			depositconfirmmenu.setTextAlignment(TextAlignment.CENTER);
			depositconfirmcontainer.setAlignment(Pos.CENTER);
			depositconfirmcontainer.getChildren().add(depositconfirmlabel);
			depositconfirmcontainer.getChildren().add(depositconfirmmenu);
			panes[7].setCenter(depositconfirmcontainer);
			
			receiptlabel = new Text("WOULD YOU LIKE A RECEIPT\n FOR YOUR TRANSACTION:\n");
			receiptmenu = new Text("CONFIRM\n\nCANCEL");
			receiptcontainer = new VBox();
			
			receiptlabel.setFont(Font.font("Arial Black", 25));
			receiptlabel.setFill(Color.WHITE);
			receiptlabel.setTextAlignment(TextAlignment.CENTER);
			receiptmenu.setFont(Font.font("Arial Black", 15));
			receiptmenu.setFill(Color.WHITE);
			receiptmenu.setTextAlignment(TextAlignment.CENTER);
			receiptcontainer.getChildren().add(receiptlabel);
			receiptcontainer.getChildren().add(receiptmenu);
			receiptcontainer.setAlignment(Pos.CENTER);
			panes[8].setCenter(receiptcontainer);
			
			transactagainlabel = new Text("WOULD YOU LIKE\n ANOTHER TRANSACTION: \n");
			transactagainmenu = new Text("CONFIRM\n\nCANCEL");
			transactagaincontainer = new VBox();
			
			transactagainlabel.setFont(Font.font("Arial Black", 25));
			transactagainlabel.setFill(Color.WHITE);
			transactagainlabel.setTextAlignment(TextAlignment.CENTER);
			transactagainmenu.setFont(Font.font("Arial Black", 15));
			transactagainmenu.setFill(Color.WHITE);
			transactagainmenu.setTextAlignment(TextAlignment.CENTER);
			transactagaincontainer.getChildren().add(transactagainlabel);
			transactagaincontainer.getChildren().add(transactagainmenu);
			transactagaincontainer.setAlignment(Pos.CENTER);
			panes[9].setCenter(transactagaincontainer);
			//Initial update.
			update();
		}
		
		public void update(){
			state = model.getState();
			for(int i=0; i<20; i++){
				if(i==state){
					bpane.setCenter(panes[i]);
				}
			}
		}
		
}

//DialPad class to use with a variety of projects.
class DialPad{
	int padinteger = 0;
	Button confirm;
	Button cancel;
	Button clear;
	Button CardDemo;
	Button Deposit100;
	Button numeric1;
	Button numeric2;
	Button numeric3;
	Button numeric4;
	Button numeric5;
	Button numeric6;
	Button numeric7;
	Button numeric8;
	Button numeric9;
	Button numeric0;
	HBox[] ButtonRows;
	VBox ButtonColumn;
	StackPane pane;
	Scene DialScene;
	//method to build our varibales.
	public void buildPad(){
		confirm = new Button("Confirm");
		cancel = new Button("Cancel");
		clear = new Button("Clear");
		CardDemo = new Button("CardDemo");
		Deposit100 = new Button("Dep$100");
		numeric1 = new Button("1");
		numeric2 = new Button("2");
		numeric3 = new Button("3");
		numeric4 = new Button("4");
		numeric5 = new Button("5");
		numeric6 = new Button("6");
		numeric7 = new Button("7");
		numeric8 = new Button("8");
		numeric9 = new Button("9");
		numeric0 = new Button("0");
		ButtonColumn = new VBox();
		pane = new StackPane();
		DialScene = new Scene(pane, 270, 350);
		ButtonRows = new HBox[14];
		for(int i=0; i< ButtonRows.length; i++){
			ButtonRows[i] = new HBox();
		}
		confirm.setMinSize(90, 70);
		cancel.setMinSize(90, 70);
		clear.setMinSize(90, 70);
		CardDemo.setMinSize(90, 70);
		Deposit100.setMinSize(90, 70);
		numeric1.setMinSize(90, 70);
		numeric2.setMinSize(90, 70);
		numeric3.setMinSize(90, 70);
		numeric4.setMinSize(90, 70);
		numeric5.setMinSize(90, 70);
		numeric6.setMinSize(90, 70);
		numeric7.setMinSize(90, 70);
		numeric8.setMinSize(90, 70);
		numeric9.setMinSize(90, 70);
		numeric0.setMinSize(90, 70);
		
		
		ButtonRows[0].getChildren().add(confirm);
		ButtonRows[0].getChildren().add(cancel);
		ButtonRows[0].getChildren().add(clear);
		ButtonRows[1].getChildren().add(numeric1);
		ButtonRows[1].getChildren().add(numeric2);
		ButtonRows[1].getChildren().add(numeric3);
		ButtonRows[2].getChildren().add(numeric4);
		ButtonRows[2].getChildren().add(numeric5);
		ButtonRows[2].getChildren().add(numeric6);
		ButtonRows[3].getChildren().add(numeric7);
		ButtonRows[3].getChildren().add(numeric8);
		ButtonRows[3].getChildren().add(numeric9);
		ButtonRows[4].getChildren().add(CardDemo);
		ButtonRows[4].getChildren().add(numeric0);
		ButtonRows[4].getChildren().add(Deposit100);
		
		ButtonColumn.getChildren().add(ButtonRows[0]);
		ButtonColumn.getChildren().add(ButtonRows[1]);
		ButtonColumn.getChildren().add(ButtonRows[2]);
		ButtonColumn.getChildren().add(ButtonRows[3]);
		ButtonColumn.getChildren().add(ButtonRows[4]);
		
		pane.getChildren().add(ButtonColumn);
	}
}
//The ATMController interface designed to work with ATMFactories.
interface ATMControllerInterface{
	void cancel();
	void update(int x);
	void setPin(int x);
	void anotherTransaction();
	void printreceipt(boolean print);
	boolean cardSwiped(int x);
	boolean withdraw(int x);
	boolean member();
	double getBalance();
	boolean eatcard();
	boolean deposit(int x);
	boolean rebukeDeposit();
	ATMView getView();
	
}
//Actual implementation.
class ATMController implements ATMControllerInterface{
	//In our controller, not only do we have our model, but we've also added  necessary atm commands that are executuable through the controller.
	ATMInterface model;
	ATMView view;
	CardReader cardreader;
	EatCardCommand eatcard;
	ReadCardCommand readcard;
	Depositor depositor;
	DepositCommand deposit;
	RebukeDepositCommand rebuke;
	//setting the model and creating view inside the controller.
	public ATMController(ATMInterface model){
		this.model = model;
		//The view needs a controller, this controller.
		view = new ATMView(this, model);
		cardreader = new CardReader(model);
		eatcard = new EatCardCommand(cardreader);
		readcard = new ReadCardCommand(cardreader);
		depositor = new Depositor(model);
		deposit = new DepositCommand(depositor);
		rebuke = new RebukeDepositCommand(depositor);
	}
	//See ATM for deeper explanation of methods, all methods call ATM model methods.
	public void anotherTransaction(){
		model.anotherTransaction();
	}
	public boolean rebukeDeposit(){
		return rebuke.execute();
	}
	public boolean eatcard(){
		return eatcard.execute();
	}
	public boolean member(){
		return model.member();
	}
	public void printreceipt(boolean print){
		model.printreceipt(print);
	}
	public boolean deposit(int x){
		return deposit.execute(x);
	}
	public boolean withdraw(int x){
		return model.withdraw(x);
	}
	public void update(int x){
		model.update(x);
	}
	public double getBalance(){
		return model.getBalance();
	}
	public ATMView getView(){
		return view;
	}
	public void cancel(){
		model.cancel();
	}
	public boolean cardSwiped(int x){
		return readcard.execute(x);
	}
	public void setPin(int x){
		model.setPin(x);
	}
}
//The default interface required to be a StateObserver.
interface StateObserver{
	void update();
}
//Generic ATMCommand interface has 3 commands possible, however, in most cases only 1 will be of use, the others are null executions.
//All commands mock physical entities that naturally come with an ATM, these are the classes/methods designed to control those physical entities.
//Since there are obviously no physical entities, we have generic print statements.
interface ATMCommand{
	public boolean execute();
	public boolean execute(int i);
	public boolean execute(String s);
}
//A simulated card reader
class CardReader{
	//Notice we have a model here, however we are not directly controlling this model, we wait for our controller to tell us to do it.
	ATMInterface model;
	public CardReader(ATMInterface model){
		this.model = model;
	}
	public boolean readCard(int x){
		System.out.println("Card Reader activated.");
		return model.cardSwiped(x);
	}
	public boolean eatCard(){
		System.out.print("Card Reader eat activated, your card is ours! Agh hah hah hah!");
		return true;
	}
}
//Read card command.
class ReadCardCommand implements ATMCommand{
	CardReader cr;
	ReadCardCommand(CardReader cr){
		this.cr = cr;
	}
	public boolean execute(int i){
		return cr.readCard(i);
	}
	public boolean execute(){
		System.out.println("NULL");
		return false;
	}
	public boolean execute(String s){
		System.out.println("NULL");
		return false;
	}
}
//Eat card command.
class EatCardCommand implements ATMCommand{
	CardReader cr;
	EatCardCommand(CardReader cr){
		this.cr = cr;
	}
	public boolean execute(){
		return cr.eatCard();
	}
	public boolean execute(int i){
		System.out.println("NULL");
		return false;
	}
	public boolean execute(String s){
		System.out.println("NULL");
		return false;
	}
}
//Simulated receiptprinter
class ReceiptPrinter{
	public boolean printReceipt(String s){
		System.out.println("Receipt Printer: \n" +s);
		return true;
	}
}
//Print receipt command.
class PrintReceiptCommand implements ATMCommand{
	ReceiptPrinter rp;
	PrintReceiptCommand(ReceiptPrinter rp){
		this.rp = rp;
	}
	public boolean execute(String s){
		return rp.printReceipt(s);
	}
	public boolean execute(int i){
		System.out.println("NULL");
		return false;
	}
	public boolean execute(){
		System.out.println("NULL");
		return false;
	}
}
//Simulated Depositor.
class Depositor{
	ATMInterface model;
	int x = 0;
	public boolean deposit(int x){
		if(this.x==0){
		System.out.println("Depositor: I'm depositing");
		this.x = x;
		}
		else{
			this.x=0;
		}
		return model.deposit(x);
	}
	public boolean rebuke(){
		System.out.println("Depositor: Fine, take your money back.");
		return true;
	}
	public Depositor(ATMInterface model){
		this.model = model;
	}
}
//Deposit command.
class DepositCommand implements ATMCommand{
	Depositor d;
	DepositCommand(Depositor d){
		this.d = d;
	}
	public boolean execute(int x){
		return d.deposit(x);
	}
	public boolean execute(){
		System.out.println("NULL");
		return false;
	}
	public boolean execute(String s){
		System.out.println("NULL");
		return false;
	}
}
//Rebuke deposit command.
class RebukeDepositCommand implements ATMCommand{
	Depositor d;
	RebukeDepositCommand(Depositor d){
		this.d = d;
	}
	public boolean execute(int x){
		System.out.println("NULL");
		return false;
	}
	public boolean execute(){
		return d.rebuke();
	}
	public boolean execute(String s){
		System.out.println("NULL");
		return false;
	}
}
//Simulated money dispenser.
class Dispenser{
	public boolean Dispense(int x){
		System.out.println("Dispenser: I'm spitting out: $"+x);
		return true;
	}
}
//Dispense command.
class DispenseCommand implements ATMCommand{
	Dispenser d;
	DispenseCommand(Dispenser d){
		this.d = d;
	}
	public boolean execute(int x){
		return d.Dispense(x);
	}
	public boolean execute(){
		System.out.println("NULL");
		return false;
	}
	public boolean execute(String s){
		System.out.println("NULL");
		return false;
	}
}

interface BankCompany {
	//1. check pin to matching card input
	boolean verify(int pin, int cardNo);
	//view current balance in account
	double getBalance();
	//check and withdraw amount from account, if available for withdrawal
	boolean Withdraw(double amount);
	//deposit amount to account
	boolean Deposit(double amount);
}

class NullBank implements BankCompany{
	public boolean verify(int pin, int cardNo){
		return false;
	}
	public double getBalance(){
		return 0;
	}
	public boolean Withdraw(double amount){
		return false;
	}
	public boolean Deposit(double amount){
		return false;
	}
}

class fromBankOfAmerica implements BankCompany{
	//User's account number; it is set to a certain card number and pin
	int userAccount;
	//money amount in account
	double userBalance = 0;
	//verified bool
	boolean verified = false;
	
	//balances as a set number to be updated
	double acc1Balance = 1000;
	double acc2Balance = 50;

	@Override
	public boolean verify(int pin, int cardNo) {
		//Make sure verified is false to begin
		verified = false;
		//Accounts Set
		int account1 = 03333;
		int account2 = 13333;
		
		//checking card Numbers and connecting them with their accounts
		
		if(cardNo == 300000){
			if(pin == 3000){
				userAccount = account1;
				userBalance = acc1Balance;
				verified = true;
				return verified;
			}else{
				verified = false;
				return verified;
			}
		}else if(cardNo == 311111){
			if(pin == 3111){
				userAccount = account2;
				userBalance = acc2Balance;
				verified = true;
				return verified;
			}else{
				verified = false;
				return verified;
			}
		}else{
			//the card is from BankOfAmerica but no account set to it-----
			System.out.println("This card does not have an account/balance from Bank Of Anerica"); 
			verified = false;
			return verified;
		}
	
	}	
	//returns current balance
	@Override
	public double getBalance() {
		if (verified){
			return userBalance;
		}else{
			return -1;
		}
		
	}

	
	@Override
	public boolean Withdraw(double amount) {
		if (verified){
			if(getBalance() >= amount){
				userBalance = getBalance() - amount;
				return true;
			}else{
				System.out.println("USER does not have enough funds to withdraw.\nCurrent Balance: " + getBalance() + "\nTotal wishing to withdraw: " + amount);
				return false;
			}
		}else{
			System.out.println("USER is not verified. USER must go through verify(String pin, String cardNo) first."); 
			return false;
		}
		
	}
	@Override
	public boolean Deposit(double amount) {
		if (verified){
			if(getBalance() >= amount){
				userBalance = getBalance() + amount;
				return true;
			}else{
				System.out.println("USER does not have enough funds to withdraw.\nCurrent Balance: " + getBalance() + "\nTotal wishing to withdraw: " + amount);
				return false;
			}
		}else{
			System.out.println("USER is not verified. USER must go through verify(String pin, String cardNo) first."); 
			return false;
		}
		
	}
	

}

class FromBBT implements BankCompany{
	//User's account number; it is set to a certain card number and pin
	int userAccount;
	//money amount in account
	double userBalance = 0;
	//verified bool
	boolean verified = false;
	
	//balances as a set number to be updated
		double acc1Balance = 15000;
		double acc2Balance = 100;
		
	@Override
	public boolean verify(int pin, int cardNo) {
		//Make sure verified is false to begin
		verified = false;
		//Accounts Set
		int account1 = 01111;
		int account2 = 10000;
		
		//checking card Numbers and connecting them with their accounts
		
		if(cardNo == 100000){
			if(pin == 1000){
				userAccount = account1;
				userBalance = acc1Balance;
				verified = true;
				return verified;
			}else{
				verified = false;
				return verified;
			}
		}else if(cardNo == 111111){
			if(pin == 1111){
				userAccount = account2;
				userBalance = acc2Balance;
				verified = true;
				return verified;
			}else{
				verified = false;
				return verified;
			}
		}else{
			//the card is from BBT but no account set to it-----
			System.out.println("This card does not have an account/balance from BBT"); 
			verified = false;
			return verified;
		}
	}

	//returns current balance
	@Override
	public double getBalance() {
		if (verified){
			return userBalance;
		}else{
			System.out.println("USER is not verified. USER must go through verify(String pin, String cardNo) first."); 
			return -1;
		}
		
	}

	@Override
	public boolean Withdraw(double amount) {
		if (verified){
			if(getBalance() >= amount){
				userBalance = getBalance() - amount;
				return true;
			}else{
				System.out.println("USER does not have enough funds to withdraw.\nCurrent Balance: " + getBalance() + "\nTotal wishing to withdraw: " + amount);
				return false;
			}
		}else{
			System.out.println("USER is not verified. USER must go through verify(String pin, String cardNo) first."); 
			return false;
		}
		
	}
	@Override
	public boolean Deposit(double amount) {
		if (verified){
			if(getBalance() >= amount){
				userBalance = getBalance() + amount;
				return true;
			}else{
				System.out.println("USER does not have enough funds to withdraw.\nCurrent Balance: " + getBalance() + "\nTotal wishing to withdraw: " + amount);
				return false;
			}
		}else{
			System.out.println("USER is not verified. USER must go through verify(String pin, String cardNo) first."); 
			return false;
		}
		
	}
	
	
}
class fromSuntrust implements BankCompany{
	//User's account number; it is set to a certain card number and pin
	int userAccount;
	//money amount/balance in account
	double userBalance = 0;
	//verified bool
	boolean verified = false;
	
	//balances as a set number to be updated
		double acc1Balance = 1300;
		double acc2Balance = 2000;
		
	@Override
	public boolean verify(int pin, int cardNo) {
		//Make sure verified is false to begin
		verified = false;
		//Accounts Set
		int account1 = 02222;
		int account2 = 12222;
		
		//checking card Numbers and connecting them with their accounts
		
		if(cardNo == 200000){
			if(pin == 2000){
				userAccount = account1;
				userBalance = acc1Balance;
				verified = true;
				return verified;
			}else{
				verified = false;
				return verified;
			}
		}else if(cardNo == 211111){
			if(pin == 2111){
				userAccount = account2;
				userBalance = acc2Balance;
				verified = true;
				return verified;
			}else{
				verified = false;
				return verified;
			}
		}else{
			//the card is from Suntrust but no account set to it-----
			System.out.println("This card does not have an account/balance from Suntrust"); 
			verified = false;
			return verified;
		}
	
	}

	//returns current balance
	@Override
	public double getBalance() {
		if (verified){
			return userBalance;
		}else{
			return -1;
		}
		
	}

	@Override
	public boolean Withdraw(double amount) {
		if (verified){
			if(getBalance() >= amount){
				userBalance = getBalance() - amount;
				return true;
			}else{
				System.out.println("USER does not have enough funds to withdraw.\nCurrent Balance: " + getBalance() + "\nTotal wishing to withdraw: " + amount);
				return false;
			}
		}else{
			System.out.println("USER is not verified. USER must go through verify(String pin, String cardNo) first."); 
			return false;
		}
		
	}
	@Override
	public boolean Deposit(double amount) {
		if (verified){
			if(getBalance() >= amount){
				userBalance = getBalance() + amount;
				return true;
			}else{
				System.out.println("USER does not have enough funds to withdraw.\nCurrent Balance: " + getBalance() + "\nTotal wishing to withdraw: " + amount);
				return false;
			}
		}else{
			System.out.println("USER is not verified. USER must go through verify(String pin, String cardNo) first."); 
			return false;
		}
		
	}
}
