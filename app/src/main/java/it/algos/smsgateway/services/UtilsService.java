package it.algos.smsgateway.services;

import android.content.Context;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import it.algos.smsgateway.AppContainer;
import it.algos.smsgateway.R;
import it.algos.smsgateway.SmsGatewayApp;

public class UtilsService {

    private Context context;

    public UtilsService(Context context) {
        this.context = context;
    }

    public String buildUrl(Context context, String path) {

        String protocol;
        if (getPrefsService().getBoolean(R.string.usessl)) {
            protocol = "https";
        } else {
            protocol = "http";
        }

        String host = getPrefsService().getString(R.string.host);

        String port = getPrefsService().getString(R.string.port);

        String url = protocol + "://" + host + ":" + port + "/" + path;

        return url;

    }

    /**
     * Validate a phone number string.
     * <br>
     *
     * @param sNumber the phone number string
     * @return a phone number object
     * @throws NumberParseException if not a valid number
     */
    public Phonenumber.PhoneNumber validatePhoneNumber(String sNumber) throws NumberParseException {

        // minumum length
        if (sNumber.length() < 6) {
            throw new NumberParseException(NumberParseException.ErrorType.TOO_SHORT_NSN, "the number " + sNumber + " is too short (min=6)");
        }

        // check that is a phone number (provide international prefix if missing)
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(sNumber, "IT");

        // check that is italian
        int countryCode = phoneNumber.getCountryCode();
        if (countryCode != 39) {
            throw new NumberParseException(NumberParseException.ErrorType.INVALID_COUNTRY_CODE, "foreign country code: " + countryCode + " (should be 39)");
        }

        return phoneNumber;

    }


    public PrefsService getPrefsService() {
        AppContainer appContainer = ((SmsGatewayApp) context).appContainer;
        return appContainer.getPrefsService();
    }

}
