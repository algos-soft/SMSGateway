package it.algos.smsgateway;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {

    private Utils utils;

    @Mock
    private Context context;


    @Before
    public void init() {
        utils=new Utils();
    }


    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    // valid number
    @Test
    public void testValidatePhoneNumber1() throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber = utils.validatePhoneNumber("3398718260");
        assertEquals(phoneNumber.getNationalNumber(), 3398718260l);
        assertEquals(phoneNumber.getCountryCode(), 39);
    }

    // valid number with prefix
    @Test
    public void testValidatePhoneNumber2() throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber = utils.validatePhoneNumber("+393357942522");
        assertEquals(phoneNumber.getNationalNumber(), 3357942522l);
        assertEquals(phoneNumber.getCountryCode(), 39);
    }

    // valid number with prefix
    @Test
    public void testValidatePhoneNumber3() throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber = utils.validatePhoneNumber("00393398718260");
        assertEquals(phoneNumber.getNationalNumber(), 3398718260l);
        assertEquals(phoneNumber.getCountryCode(), 39);
    }

    // valid number with space
    @Test
    public void testValidatePhoneNumber4() throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber = utils.validatePhoneNumber("339 5362229");
        assertEquals(phoneNumber.getNationalNumber(), 3395362229l);
        assertEquals(phoneNumber.getCountryCode(), 39);
    }

    // valid number with dash
    @Test
    public void testValidatePhoneNumber5() throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber = utils.validatePhoneNumber("349-6307790");
        assertEquals(phoneNumber.getNationalNumber(), 3496307790l);
        assertEquals(phoneNumber.getCountryCode(), 39);
    }



    // bab international prefix
    @Test(expected = NumberParseException.class)
    public void testValidatePhoneNumber10() throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber = utils.validatePhoneNumber("00413388645789");
    }

    // too short
    @Test(expected = NumberParseException.class)
    public void testValidatePhoneNumber11() throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber = utils.validatePhoneNumber("12345");
    }

    // no digits
    @Test(expected = NumberParseException.class)
    public void testValidatePhoneNumber12() throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber = utils.validatePhoneNumber("abcdefghijkl");
    }




}