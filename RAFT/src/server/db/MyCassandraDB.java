package server.db;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.Bytes;

public class MyCassandraDB implements DatabaseClient{

	
	Cluster cluster;
	Session session;
	
	public MyCassandraDB(String url, String username,String password, String db, String ssl) {
		cluster = Cluster.builder().addContactPoint(url).build();
		session = cluster.connect(db);
	}
	
	
	@Override
	public byte[] get(String key) {
		Statement stmt = null;
		byte[] image=null; 
		ByteBuffer img=null;
		byte[] temp = null;
		try {
			PreparedStatement ps=session.prepare("Select image FROM tablename WHERE key = ?");
			BoundStatement bs=new BoundStatement(ps);
			com.datastax.driver.core.ResultSet rs = session.execute(bs.bind(key));
			 List<ByteBuffer> ls=null;
	        for (Row row : rs) {
	        	img=row.getBytes("image");
	        	temp=Bytes.getArray(img);
	        	}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// TODO connection handling
		}
		
		return image;	
	}

	@Override
	public String post(byte[] image, long timestamp) throws SQLException {
		String key = UUID.randomUUID().toString();
		try {
			System.out.write(image);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ByteBuffer img= ByteBuffer.wrap(image);
		
		PreparedStatement ps=session.prepare("INSERT INTO tablename ( key , image , timestamp ) VALUES (?, ?, ?);");
		BoundStatement bs=new BoundStatement(ps);
		session.execute(bs.bind(key,img,timestamp));
		System.out.println("Inserted "+ key);
		return key;
	}

	@Override
	public void put(String key, byte[] image, long timestamp) {
		PreparedStatement ps = null;
		try {
			ps = session.prepare("UPDATE testtable SET image= ? , timestamp = ?  WHERE key LIKE ?");
			BoundStatement bs=new BoundStatement(ps);
			ByteBuffer img= ByteBuffer.wrap(image);
			session.execute(bs.bind(key,img,timestamp));
			
		} finally {
		}
		
	}

	@Override
	public void delete(String key) {
		Statement stmt = null;
		try {
			PreparedStatement ps= session.prepare("DELETE FROM testtable WHERE key = ? ;");			
			BoundStatement bs=new BoundStatement(ps);
			
			session.execute(bs.bind(key));
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// TODO close connection, decide if need to keep open all time or
			// initiate new everytime
		}
	}
		
	

	@Override
	public long getCurrentTimeStamp() {
		Statement stmt = null;
		long timestamp = 0; 
		try {
			PreparedStatement ps= session.prepare("Select max(timestamp) FROM testtable");			
			BoundStatement bs=new BoundStatement(ps);
			com.datastax.driver.core.ResultSet rs = session.execute(bs);
	        for (Row row : rs) {
	            timestamp = row.getLong(1);
	        }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// TODO connection handling
		}
		return timestamp;	
	}

	@Override
	public List<Record> getNewEntries(long staleTimestamp) {
		Statement stmt = null;
		List<Record> list = new ArrayList<Record>();
		try {
			PreparedStatement ps= session.prepare("Select key, image, timestamp FROM testtable where timestamp > ?");			
			BoundStatement bs=new BoundStatement(ps);
			com.datastax.driver.core.ResultSet rs = session.execute(bs);
			for (Row row : rs) {
				list.add(new Record(row.getString("key"), Bytes.getArray(row.getBytes("image")), row.getLong("timestamp")));
	        }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// TODO connection handling
		}
		return list;
	}

	@Override
	public void putEntries(List<Record> list) {
		for (Record record : list) {
			put(record.getKey(), record.getImage(), record.getTimestamp());
		}
	}

	@Override
	public List<Record> getAllEntries() {
		Statement stmt = null;
		List<Record> list = new ArrayList<Record>();
		try {
			PreparedStatement ps= session.prepare("Select key, image, timestamp FROM testtable");			
			BoundStatement bs=new BoundStatement(ps);
			com.datastax.driver.core.ResultSet rs = session.execute(bs);
			for (Row row : rs) {
				list.add(new Record(row.getString("key"), Bytes.getArray(row.getBytes("image")), row.getLong("timestamp")));
	        }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// TODO connection handling
		}
		return list;	
	}

	@Override
	public void post(String key, byte[] image, long timestamp) {
		PreparedStatement ps = null;
		try {
			ps = session.prepare("UPDATE testtable SET image= ? , timestamp = ?  WHERE key LIKE ?");
			BoundStatement bs=new BoundStatement(ps);
			ByteBuffer img= ByteBuffer.wrap(image);
			com.datastax.driver.core.ResultSet rs = session.execute(bs);
			session.execute(bs.bind(key,img,timestamp));
		} finally {
		}
	}

}