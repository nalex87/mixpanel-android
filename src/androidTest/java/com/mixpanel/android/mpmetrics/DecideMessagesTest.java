package com.mixpanel.android.mpmetrics;

import android.test.AndroidTestCase;

import com.mixpanel.android.viewcrawler.UpdatesFromMixpanel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DecideMessagesTest extends AndroidTestCase {

    @Override
    public void setUp() throws JSONException, BadDecideObjectException {

        mListenerCalls = new LinkedBlockingQueue<String>();
        mMockListener = new DecideMessages.OnNewResultsListener() {
            @Override
            public void onNewResults() {
                mListenerCalls.add("CALLED");
            }
        };

        mMockUpdates = new UpdatesFromMixpanel() {
            @Override
            public void startUpdates() {
                ; // do nothing
            }

            @Override
            public void setEventBindings(JSONArray bindings) {
                ; // TODO should observe bindings here
            }

            @Override
            public void setVariants(JSONArray variants) {
                ; // TODO should observe this
            }

            @Override
            public Tweaks getTweaks() {
                return null;
            }

            @Override
            public void addOnMixpanelTweaksUpdatedListener(OnMixpanelTweaksUpdatedListener listener) {

            }

            @Override
            public void removeOnMixpanelTweaksUpdatedListener(OnMixpanelTweaksUpdatedListener listener) {

            }
        };

        mDecideMessages = new DecideMessages("TEST TOKEN", mMockListener, mMockUpdates);
        mSomeSurveys = new ArrayList<>();
        mSomeNotifications = new ArrayList<>();

        JSONObject surveyDesc1 = new JSONObject(
                "{\"collections\":[{\"id\":1,\"selector\":\"true\"}],\"id\":1,\"questions\":[{\"prompt\":\"a\",\"extra_data\":{\"$choices\":[\"1\",\"2\"]},\"type\":\"multiple_choice\",\"id\":1}]}"
        );

        JSONObject surveyDesc2 = new JSONObject(
                "{\"collections\":[{\"id\":2,\"selector\":\"true\"}],\"id\":2,\"questions\":[{\"prompt\":\"a\",\"extra_data\":{\"$choices\":[\"1\",\"2\"]},\"type\":\"multiple_choice\",\"id\":2}]}"
        );
        mSomeSurveys.add(new Survey(surveyDesc1));
        mSomeSurveys.add(new Survey(surveyDesc2));

        JSONObject notifsDesc1 = new JSONObject(
                "{\"id\": 1234, \"message_id\": 4321, \"type\": \"takeover\", \"body\": \"Hook me up, yo!\", \"body_color\": 4294901760, \"title\": null, \"title_color\": 4278255360, \"image_url\": \"http://mixpanel.com/Balok.jpg\", \"bg_color\": 3909091328, \"close_color\": 4294967295, \"extras\": {\"image_fade\": true},\"buttons\": [{\"text\": \"Button!\", \"text_color\": 4278190335, \"bg_color\": 4294967040, \"border_color\": 4278255615, \"cta_url\": \"hellomixpanel://deeplink/howareyou\"}, {\"text\": \"Button 2!\", \"text_color\": 4278190335, \"bg_color\": 4294967040, \"border_color\": 4278255615, \"cta_url\": \"hellomixpanel://deeplink/howareyou\"}]}"
        );
        JSONObject notifsDesc2 = new JSONObject(
                "{\"body\":\"A\",\"image_tint_color\":4294967295,\"border_color\":4294967295,\"message_id\":85151,\"bg_color\":3858759680,\"extras\":{},\"image_url\":\"https://cdn.mxpnl.com/site_media/images/engage/inapp_messages/mini/icon_megaphone.png\",\"cta_url\":null,\"type\":\"mini\",\"id\":1191793,\"body_color\":4294967295}"
        );

        mSomeNotifications.add(new TakeoverInAppNotification(notifsDesc1));
        mSomeNotifications.add(new MiniInAppNotification(notifsDesc2));

        mSomeBindings = new JSONArray(); // TODO need some bindings
        mSomeVariants = new JSONArray(); // TODO need some variants
    }

    public void testDuplicateIds() throws JSONException, BadDecideObjectException {
        mDecideMessages.reportResults(mSomeSurveys, mSomeNotifications, mSomeBindings, mSomeVariants);

        final List<Survey> fakeSurveys = new ArrayList<Survey>(mSomeSurveys.size());
        for (final Survey real: mSomeSurveys) {
            fakeSurveys.add(new Survey(new JSONObject(real.toJSON())));
            assertEquals(mDecideMessages.getSurvey(false), real);
        }

        final List<InAppNotification> fakeNotifications = new ArrayList<InAppNotification>(mSomeNotifications.size());
        for (final InAppNotification real: mSomeNotifications) {
            if (real.getClass().isInstance(TakeoverInAppNotification.class)) {
                fakeNotifications.add(new TakeoverInAppNotification(new JSONObject(real.toString())));
            } else if (real.getClass().isInstance(MiniInAppNotification.class)) {
                fakeNotifications.add(new MiniInAppNotification(new JSONObject(real.toString())));
            }
            assertEquals(mDecideMessages.getNotification(false), real);
        }

        assertNull(mDecideMessages.getSurvey(false));
        assertNull(mDecideMessages.getNotification(false));

        mDecideMessages.reportResults(fakeSurveys, fakeNotifications, mSomeBindings, mSomeVariants);

        assertNull(mDecideMessages.getSurvey(false));
        assertNull(mDecideMessages.getNotification(false));

        JSONObject surveyNewIdDesc = new JSONObject(
                "{\"collections\":[{\"id\":1,\"selector\":\"true\"}],\"id\":1001,\"questions\":[{\"prompt\":\"a\",\"extra_data\":{\"$choices\":[\"1\",\"2\"]},\"type\":\"multiple_choice\",\"id\":1}]}"
        );
        final Survey unseenSurvey = new Survey(surveyNewIdDesc);
        fakeSurveys.add(unseenSurvey);

        JSONObject notificationNewIdDesc = new JSONObject(
                "{\"body\":\"A\",\"image_tint_color\":4294967295,\"border_color\":4294967295,\"message_id\":85151,\"bg_color\":3858759680,\"extras\":{},\"image_url\":\"https://cdn.mxpnl.com/site_media/images/engage/inapp_messages/mini/icon_megaphone.png\",\"cta_url\":null,\"type\":\"mini\",\"id\":1,\"body_color\":4294967295}"
        );
        final InAppNotification unseenNotification = new MiniInAppNotification(notificationNewIdDesc);
        fakeNotifications.add(unseenNotification);

        mDecideMessages.reportResults(fakeSurveys, fakeNotifications, mSomeBindings, mSomeVariants);

        assertEquals(mDecideMessages.getSurvey(false), unseenSurvey);
        assertEquals(mDecideMessages.getNotification(false), unseenNotification);

        assertNull(mDecideMessages.getSurvey(false));
        assertNull(mDecideMessages.getNotification(false));
    }

    public void testPops() {
        final Survey nullBeforeSurvey = mDecideMessages.getSurvey(false);
        assertNull(nullBeforeSurvey);

        final InAppNotification nullBeforeNotification = mDecideMessages.getNotification(false);
        assertNull(nullBeforeNotification);

        mDecideMessages.reportResults(mSomeSurveys, mSomeNotifications, mSomeBindings, mSomeVariants);

        final Survey s1 = mDecideMessages.getSurvey(false);
        assertEquals(mSomeSurveys.get(0), s1);

        final Survey s2 = mDecideMessages.getSurvey(false);
        assertEquals(mSomeSurveys.get(1), s2);

        final Survey shouldBeNullSurvey = mDecideMessages.getSurvey(false);
        assertNull(shouldBeNullSurvey);

        final InAppNotification n1 = mDecideMessages.getNotification(false);
        assertEquals(mSomeNotifications.get(0), n1);

        final InAppNotification n2 = mDecideMessages.getNotification(false);
        assertEquals(mSomeNotifications.get(1), n2);

        final InAppNotification shouldBeNullNotification = mDecideMessages.getNotification(false);
        assertNull(shouldBeNullNotification);
    }

    public void testListenerCalls() throws JSONException, BadDecideObjectException {
        assertNull(mListenerCalls.peek());
        mDecideMessages.reportResults(mSomeSurveys, mSomeNotifications, mSomeBindings, mSomeVariants);
        assertEquals(mListenerCalls.poll(), "CALLED");
        assertNull(mListenerCalls.peek());

        // No new info means no new calls
        mDecideMessages.reportResults(mSomeSurveys, mSomeNotifications, mSomeBindings, mSomeVariants);
        assertNull(mListenerCalls.peek());

        // New info means new calls
        JSONObject notificationNewIdDesc = new JSONObject(
                "{\"body\":\"A\",\"image_tint_color\":4294967295,\"border_color\":4294967295,\"message_id\":85151,\"bg_color\":3858759680,\"extras\":{},\"image_url\":\"https://cdn.mxpnl.com/site_media/images/engage/inapp_messages/mini/icon_megaphone.png\",\"cta_url\":null,\"type\":\"mini\",\"id\":1,\"body_color\":4294967295}"
        );
        final InAppNotification unseenNotification = new MiniInAppNotification(notificationNewIdDesc);
        final List<InAppNotification> newNotifications = new ArrayList<InAppNotification>();
        newNotifications.add(unseenNotification);

        mDecideMessages.reportResults(mSomeSurveys, newNotifications, mSomeBindings, mSomeVariants);
        assertEquals(mListenerCalls.poll(), "CALLED");
        assertNull(mListenerCalls.peek());
    }

    private BlockingQueue<String> mListenerCalls;
    private DecideMessages.OnNewResultsListener mMockListener;
    private UpdatesFromMixpanel mMockUpdates;
    private DecideMessages mDecideMessages;
    private JSONArray mSomeBindings;
    private JSONArray mSomeVariants;
    private List<Survey> mSomeSurveys;
    private List<InAppNotification> mSomeNotifications;
}
