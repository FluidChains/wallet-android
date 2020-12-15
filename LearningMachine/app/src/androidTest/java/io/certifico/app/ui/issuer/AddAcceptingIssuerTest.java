package io.certifico.app.ui.issuer;

import android.support.test.espresso.intent.rule.IntentsTestRule;

import io.certifico.app.R;

import org.junit.Rule;
import org.junit.Test;

import io.certifico.app.ui.issuer.AddIssuerActivity;
import timber.log.Timber;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by chris on 1/4/18.
 */

public class AddAcceptingIssuerTest {
    public static final String WIREMOCK_SERVER = "http://10.0.2.2:1234";

    @Rule
    public IntentsTestRule<AddIssuerActivity> mActivityTestRule = new IntentsTestRule<AddIssuerActivity>(AddIssuerActivity.class);

    @Test
    public void addAcceptingIssuerTest() {
//        onView(withId(R.id.add_issuer_url_edit_text)).perform(replaceText(WIREMOCK_SERVER + "/issuer/accepting-estimate-unsigned"), closeSoftKeyboard());
//        onView(withId(R.id.add_issuer_nonce_edit_text)).perform(replaceText("1234"), closeSoftKeyboard());
//        onView(withId(R.id.import_button)).perform(click());
//
////        intended(hasComponent(IssuerActivity.class.getName()));
////        intended(toPackage("IssuerActivity"));
//        Timber.i("Did it work??");
    }
}
