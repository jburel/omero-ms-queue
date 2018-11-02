package kew.providers.artemis.qchan;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.junit.Before;
import org.junit.Test;
import util.types.FutureTimepoint;

import java.util.Optional;


public class ArtemisMessageTest {

    private ClientMessage mockAdaptee;
    private ArtemisSessionSynchronizer mockSynchronizer;
    private ArtemisMessage target;

    @Before
    public void setup() {
        mockAdaptee = mock(ClientMessage.class);
        mockSynchronizer = mock(ArtemisSessionSynchronizer.class);
        target = new ArtemisMessage(mockAdaptee, mockSynchronizer);
    }

    @Test
    public void canUseStringProp() {
        String key = "k", value = "v";
        when(mockAdaptee.putStringProperty(key, value)).thenReturn(mockAdaptee);
        when(mockAdaptee.containsProperty(key)).thenReturn(true);
        when(mockAdaptee.getStringProperty(key)).thenReturn(value);

        target.putProp(key, value);
        Optional<String> actual = target.lookupStringValue(key);

        assertNotNull(actual);
        assertTrue(actual.isPresent());
        assertThat(actual.get(), is(value));
    }

    @Test
    public void returnEmptyIfPropNotSet() {
        Optional<String> actual = target.lookupStringValue("key");

        assertNotNull(actual);
        assertFalse(actual.isPresent());
    }

    @Test
    public void canSchedule() {
        String key = Message.HDR_SCHEDULED_DELIVERY_TIME.toString();
        target.setScheduledDeliveryTime(FutureTimepoint.now());
        verify(mockAdaptee, times(1)).putLongProperty(eq(key), anyLong());
    }

    @Test (expected = NullPointerException.class)
    public void ctorThrowsIfNullAdaptee() {
        new ArtemisMessage(null, mockSynchronizer);
    }

    @Test (expected = NullPointerException.class)
    public void ctorThrowsIfNullSynchronizer() {
        new ArtemisMessage(mockAdaptee, null);
    }

    @Test (expected = NullPointerException.class)
    public void cantPutStringPropWithNullKey() {
        target.putProp(null, "");
    }

    @Test (expected = NullPointerException.class)
    public void cantPutStringPropWithNullValue() {
        target.putProp("k", null);
    }

    @Test (expected = NullPointerException.class)
    public void cantPutLongPropWithNullKey() {
        target.putProp(null, 0);
    }

    @Test (expected = NullPointerException.class)
    public void cantLookupStringPropWithNullKey() {
        target.lookupStringValue(null);
    }

    @Test (expected = NullPointerException.class)
    public void cantLookupLongPropWithNullKey() {
        target.lookupLongValue(null);
    }

}
