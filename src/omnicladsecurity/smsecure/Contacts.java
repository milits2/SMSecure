package omnicladsecurity.smsecure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;

public class Contacts {
	List<String> numbers;
	Context context;
	
	Contacts(Context context) {
		this.context = context;
		
		numbers = new ArrayList<String>();
		loadContacts();
	}
	
	String[] getNumbersArray() {
		// Converts the list of numbers into an array for display/storage
		return numbers.toArray(new String[numbers.size()]);
	}
	
	boolean addNumber(String number) {
		// Add a number to the contacts list
		if(number.length() != 10) return false;
		
		if(numbers.contains(number)) {
			// The number already exists, so don't update anything.
			return false;
		}
		if(numbers.size() > 0) {
			numbers.add(0, number);
		}
		else {
			numbers.add(number);
		}
		
		saveContacts();
		
		return true;
	}
	
	void removeNumber(String number) {
		// Remove a number from the contacts list
		numbers.remove(number);
		saveContacts();
	}
	
	void saveContacts() {
		// Store the contacts list in sharedpreferences.
		String[] lNumbers = getNumbersArray();
		
		SharedPreferences prefs = context.getSharedPreferences("contactsBook", Context.MODE_PRIVATE);
		SharedPreferences.Editor writer = prefs.edit();
		
		// Remove old numbers
		int oldSize = prefs.getInt("contactsListSize", 0);
		for(int i = 0; i < oldSize; ++i) {
			writer.remove("contacts_" + i);
		}
		writer.commit();
		
		// Store new numbers
		writer.putInt("contactsListSize", lNumbers.length);		
		for(int i = 0; i < lNumbers.length; ++i) {
			writer.putString("contacts_" + i, lNumbers[i]);
		}
		writer.commit();
	}
	
	void loadContacts() {
		// Load the contacts list from sharedpreferences.
		SharedPreferences prefs = context.getSharedPreferences("contactsBook", Context.MODE_PRIVATE);
		int size = prefs.getInt("contactsListSize", 0);
		
		String lNumbers[] = new String[size];
		
		for(int i = 0; i < size; ++i) {
			lNumbers[i] = prefs.getString("contacts_" + i, null);
		}
		
		if(size > 0) {
			numbers = new ArrayList<String>(Arrays.asList(lNumbers));
		}
	}
}
