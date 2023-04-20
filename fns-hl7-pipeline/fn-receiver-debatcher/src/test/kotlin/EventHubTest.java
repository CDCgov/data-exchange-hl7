import com.microsoft.azure.functions.ExecutionContext;
import gov.cdc.dex.azure.EventHubMetadata;
import gov.cdc.dex.hl7.receiver.Function;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class EventHubTest {

    @Test
    public void callReceiverDebatcherFunction_happyPath(){
        Function function = new Function();
        String json ="";
        try {
            JSONParser parser = new JSONParser();
            //Use JSONObject for simple JSON and JSONArray for array of JSON.
            org.json.simple.JSONArray data = (org.json.simple.JSONArray) parser.parse(
                    new FileReader("src/test/resources/message.txt"));//path to the JSON file.

             json = data.toString();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        List<String> messages = new ArrayList<>();
        messages.add(json);
        List<EventHubMetadata> eventHubMDList = new ArrayList<>();
        EventHubMetadata eventHubMD = new EventHubMetadata(1, 99, "", "");
        eventHubMDList.add(eventHubMD);
        function.eventHubProcessor(messages, eventHubMDList, getExecutionContext());
    }




    private ExecutionContext getExecutionContext() {
        return new ExecutionContext() {
            @Override
            public Logger getLogger() {
                return Logger.getLogger(EventHubTest.class.getName());
            }

            @Override
            public String getInvocationId() {
                return null;
            }

            @Override
            public String getFunctionName() {
                return null;
            }
        };
    }

}
