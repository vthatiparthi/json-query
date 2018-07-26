package io.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class VisitorActivites implements Callable<Map<String, Long>>{
	private int groupBySize;
	private long startPos;
	private long endPos;
	private String fileName;
	
	public VisitorActivites() {
		
	}
	public VisitorActivites(int groupBySize, long startPos, long endPos, String fileName) {
		this.groupBySize = groupBySize;
		this.startPos = startPos;
		this.endPos = endPos;
		this.fileName = fileName;
	}
	
	
	private void parseActions(JsonArray actionsJosnarray, Map<String, Long> userAndTimestampMap, String pid, int groupBySize) {
		for(JsonElement action : actionsJosnarray) {
			if(action.getAsJsonObject().get("time") != null) {
				long timestamp = action.getAsJsonObject().get("time").getAsLong();
				updateMap(userAndTimestampMap, pid, timestamp, groupBySize);
			}
		}
	}
	
	static void updateMap(Map<String, Long> userAndTimestampMap, String pid, long timestamp, int groupBySize) {
		Long prevTimestamp;
		if(userAndTimestampMap.size()<=groupBySize && (prevTimestamp = userAndTimestampMap.get(pid)) != null) {
			if(prevTimestamp < timestamp) {
				userAndTimestampMap.put(pid, timestamp);
			}
		} else if(userAndTimestampMap.size() < groupBySize) {
			userAndTimestampMap.put(pid, timestamp);
		} else {
			String userToRemove = null;
			long maxtTimestamp = timestamp;
			for(String user : userAndTimestampMap.keySet()) {
				long userTimestamp = userAndTimestampMap.get(user);
				if(userTimestamp < maxtTimestamp) {
					userToRemove = user;
					maxtTimestamp = userTimestamp;
				}
			}
			if(userToRemove != null) {
				userAndTimestampMap.remove(userToRemove);
				userAndTimestampMap.put(pid, timestamp);
			}
		}
	}

	@Override
	public Map<String, Long> call() throws Exception {
		Map<String, Long> userAndTimestampMap = new HashMap<>(groupBySize);
		BufferedRandomAccessFile raf;
		try {
			raf = new BufferedRandomAccessFile(fileName, "r", 8200);
			if(startPos != 0) {
				raf.seek(startPos);
				String line = raf.readLine();
				if(line != null) {
					startPos += line.getBytes().length;
				}
			}
			while (startPos <= endPos) {
				String line = raf.readLine();
				if(line == null) {
					break;
				}
				JsonParser parser = new JsonParser();
				JsonObject jsonObject = parser.parse(line).getAsJsonObject();
				String pid = null;
				if(jsonObject.get("pid") != null) {
					pid = jsonObject.get("pid").getAsString();
				}
				if(pid == null) {
					continue;
				}
				if(jsonObject.get("actions") != null) {
					JsonArray actionsJosnarray = jsonObject.get("actions").getAsJsonArray();
					parseActions(actionsJosnarray, userAndTimestampMap, pid, groupBySize);
				}
				startPos += line.getBytes().length;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return userAndTimestampMap;
	}
}
