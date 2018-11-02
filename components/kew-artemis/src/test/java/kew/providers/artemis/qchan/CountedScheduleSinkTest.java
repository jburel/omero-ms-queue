package kew.providers.artemis.qchan;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static kew.core.msg.ChannelMessage.message;
import static util.types.FutureTimepoint.now;

import java.time.Duration;

import kew.core.qchan.impl.CountedScheduleSink;
import kew.core.qchan.impl.MetaProps;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.junit.Before;
import org.junit.Test;

import kew.core.msg.ChannelMessage;
import kew.core.msg.CountedSchedule;
import kew.core.msg.MessageSink;

public class CountedScheduleSinkTest implements MessageSink<CountedSchedule, Long> {

    private static final Long DataWhenNoMetadata = -1L;
    private CountedScheduleSink<ArtemisMessage, Long> target;
    private ChannelMessage<CountedSchedule, Long> mappedMsg;
    
    private ChannelMessage<ArtemisMessage, Long> newQueuedMsg(Long count) {
        ClientMessage qMsg = mock(ClientMessage.class);
        boolean hasProp = count != null;
        when(qMsg.containsProperty(MetaProps.ScheduleCountKey))
            .thenReturn(hasProp);
        when(qMsg.getLongProperty(MetaProps.ScheduleCountKey))
            .thenReturn(count);

        ArtemisMessage adapterMsg = new ArtemisMessage(qMsg, () -> null);
        return message(adapterMsg, hasProp ? count : DataWhenNoMetadata);
    }
    
    private void assertMapped(Long expectedCount) {
        assertNotNull(mappedMsg);
        if (expectedCount == null) {
            assertFalse(mappedMsg.metadata().isPresent());
            assertThat(mappedMsg.data(), is(DataWhenNoMetadata));
        } else {
            assertTrue(mappedMsg.metadata().isPresent());
            
            CountedSchedule mapped = mappedMsg.metadata().get();
            
            assertThat(now().get().minus(mapped.when().get()), 
                       lessThan(Duration.ofSeconds(1)));
            assertThat(mapped.count(), is(expectedCount));
        }
    }
    
    @Before
    public void setup() {
        target = new CountedScheduleSink<>(this);
    }
    
    @Override
    public void consume(ChannelMessage<CountedSchedule, Long> msg) {
        this.mappedMsg = msg;
    }
    
    @Test
    public void mapNullCountToEmptyCountedSchedule() {
        target.consume(newQueuedMsg(null));
        assertMapped(null);
    }
    
    @Test
    public void mapPositiveCountToCountedSchedule() {
        target.consume(newQueuedMsg(2L));
        assertMapped(2L);
    }
    
    @Test
    (expected = IllegalArgumentException.class)
    public void failIfCountIsPresentButNotPositive() {
        target.consume(newQueuedMsg(0L));
    }
    
}
