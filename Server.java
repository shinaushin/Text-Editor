import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

class Server {
	String data;
	String id;
	ArrayList<HandleClient> clients;
	LinkedList<Operation> opts;
	public Server() throws Exception {
		ServerSocket server = new ServerSocket(9999,10);
		clients = new ArrayList<HandleClient>();
		opts = new LinkedList<Operation>();
		System.out.println("Server Started...");
		data ="";
		new BroadcastHandler(this).start();
		while(true) {
			try {
				Socket client = server.accept(); 
				HandleClient c = new HandleClient(client, this);
				clients.add(c);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	/*
	 */
	public void accept(HandleClient c, String opt, String txt, int pos) {
		System.out.println(data);
		Operation operation = new Operation(c,opt,txt,pos);
		opts.add(operation);
		broadcast(operation);
	}
	public void broadcast(Operation o) {
		for(HandleClient c: clients) {
			if(c!=o.creator)
				c.sendUpdate(o.opt, o.txt, o.pos);
		}
	}

	public void delete (String txt, int pos) {
		if(pos<0) {
			pos = 0;
		}
		try {
			data = data.substring(0,pos)+data.substring(pos+txt.length());
		}
		catch(Exception e) {
			System.out.println(pos);
			e.printStackTrace();
		}
	}
	public void insert (String txt, int pos) {
		if(pos<0) pos = 0;
		if(pos<=data.length()) {
			data = data.substring(0,pos) + txt + data.substring(pos,data.length());
		}
		else {
			data = data + txt;
		}
	}
}
class Operation {
	String opt;
	String txt;
	int pos;
	HandleClient creator;
	public Operation(HandleClient creator, String opt, String txt, int pos) {
		this.opt = opt;
		this.txt = txt;
		this.pos = pos;
		this.creator = creator;
	}
}
class HandleClient extends Thread {
	String name;
	BufferedReader input;
	PrintWriter output;
	Server serv;
	public HandleClient(Socket client, Server serv) throws Exception {
		this.serv = serv;
		input = new BufferedReader( new InputStreamReader( client.getInputStream())) ;
		output = new PrintWriter ( client.getOutputStream(),true);
		String[] in  = input.readLine().split(",");
		name = in[0];
		System.out.println("New client " + name);
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<Integer.parseInt(in[1]);i++) {
			sb.append(" ");
		}
		if(!sb.toString().isEmpty()) {
			sendUpdate("del", sb.toString(), 0);
		}
		if(!serv.data.isEmpty()) {
			sendUpdate("ins", serv.data, 0);
		}
		start();
	}
	public void sendUpdate(String opt, String txt, int pos) {
		output.println(opt+"^"+txt+"^"+pos+"~");
	}  
	public void run() {
		try {
			StringBuilder sb = new StringBuilder();
			while(true) {
				sb.setLength(0);
				char in;
				while((in=(char)(input.read()))!='~') {
					sb.append(in);
				}
				String[] params = sb.toString().split("\\^");
				try {
					serv.accept(this, params[0].trim(), params[1], Integer.parseInt(params[2]));
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
class BroadcastHandler extends Thread {
	Server serv;
	LinkedList<Operation> opts;
	LinkedList<Operation> processing = new LinkedList<Operation>();
	public BroadcastHandler(Server serv) {
		this.serv = serv;
		opts = serv.opts;
	}
	public void run () {
		while(true) {
			try {
				int size = opts.size();
				if(!opts.isEmpty()) {
					processing.addAll(opts);
				}
				for (int i = 0; i < size; i++) {
					int change = 0;
					String operator = processing.get(i).opt.trim();
					int sizeof = processing.get(i).txt.length();
					if(operator.equals("del"))
						change -= sizeof;
					else if(operator.equals("ins"))
						change += sizeof;
					for (int j = i+1; j < size; j++) {
						if (processing.get(i).pos < processing.get(j).pos) {
							processing.get(j).pos += change;
						}
					}
					if(operator.equals("ins")){
						serv.insert(processing.get(i).txt,processing.get(i).pos);
					}
					if(operator.equals("del")){
						serv.delete(processing.get(i).txt,processing.get(i).pos);
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			while(!processing.isEmpty()) {
				opts.remove(processing.pop());
			}
		}
	}
}
