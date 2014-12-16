/*
 * Copyright 2010 sasc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sasc.emv;

import sasc.iso7816.SmartCardException;
import java.util.Arrays;
import sasc.iso7816.Iso7816Commands;
import sasc.util.Util;

/**
 * Static utility methods used to construct EMV commands
 *
 * A complete list of all EMV commands (including standard ISO7816-4 commands):
 *
 * cls  ins  command
 * '8x' '1E' APPLICATION BLOCK
 * '8x' '18' APPLICATION UNBLOCK
 * '8x' '16' CARD BLOCK
 * '0x' '82' EXTERNAL AUTHENTICATE
 * '8x' 'AE' GENERATE APPLICATION CRYPTOGRAM
 * '0x' '84' GET CHALLENGE
 * '8x' 'CA' GET DATA (ATC, Last Online ATC, PIN Try Counter, LogFormat)
 * '8x' 'A8' GET PROCESSING OPTIONS
 * '0x' '88' INTERNAL AUTHENTICATE
 * '8x' '24' PERSONAL IDENTIFICATION NUMBER (PIN) CHANGE/UNBLOCK
 * '0x' 'B2' READ RECORD
 * '0x' 'A4' SELECT
 * '0x' '20' VERIFY //PIN
 * '8x' 'Dx' RFU for the payment systems
 * '8x' 'Ex' RFU for the payment systems
 * '9x' 'xx' RFU for manufacturers for proprietary INS coding
 * 'Ex' 'xx' RFU for issuers for proprietary INS coding
 *
 *
 * @author sasc
 */
public class EMVAPDUCommands {

    public static String selectPSE() {
        return selectByDFName(Util.fromHexString("31 50 41 59 2E 53 59 53 2E 44 44 46 30 31")); //1PAY.SYS.DDF01
    }

    public static String selectPPSE() {
        return selectByDFName(Util.fromHexString("32 50 41 59 2E 53 59 53 2E 44 44 46 30 31")); //2PAY.SYS.DDF01
    }

    public static String selectByDFName(byte[] fileBytes) {
        return Iso7816Commands.selectByDFName(fileBytes);
    }
    
    public static String selectByDFNameNextOccurrence(byte[] fileBytes) {
        return Iso7816Commands.selectByDFNameNextOccurrence(fileBytes);
    }

    public static String readRecord(int recordNum, int sfi) {
        return Iso7816Commands.readRecord(recordNum, sfi);
    }

    public static String getProcessingOpts(DOL pdol) {
        String command;
        if (pdol != null && pdol.getTagAndLengthList().size() > 0) {
            byte[] pdolResponseData = EMVTerminalProfile.constructDOLResponse(pdol);
            command = "80 A8 00 00";
            command += " " + Util.int2Hex(pdolResponseData.length + 2) + " 83 " + Util.int2Hex(pdolResponseData.length);
            command += " " + Util.prettyPrintHexNoWrap(pdolResponseData);
        } else {
            command = "80 A8 00 00 02 83 00";
        }
        return command;
    }

    public static String getApplicationTransactionCounter() {
        return "80 CA 9F 36 00";
    }

    public static String getLastOnlineATCRegister() {
        return "80 CA 9F 13 00";
    }

    public static String getPINTryConter() {
        return "80 CA 9F 17 00";
    }

    public static String getLogFormat() {
        return "80 CA 9F 4F 00";
    }

    public static String internalAuthenticate(byte[] authenticationRelatedData) {
        return Iso7816Commands.internalAuthenticate(authenticationRelatedData);
    }

    public static String externalAuthenticate(byte[] cryptogram, byte[] proprietaryBytes) {
        return Iso7816Commands.externalAuthenticate(cryptogram, proprietaryBytes);
    }
    
    public static String generateAC(byte referenceControlParameterP1, byte[] transactionRelatedData) {
        return "80 AE "+referenceControlParameterP1+"00 "+Util.int2Hex(transactionRelatedData.length)+
                Util.prettyPrintHexNoWrap(transactionRelatedData)+" 00";
    }

    /**
     * The GET CHALLENGE command is used to obtain an unpredictable number from
     * the ICC for use in a security-related procedure.
     * The challenge shall be valid only for the next issued command
     *
     * The data field of the response message contains an 8-byte unpredictable number generated by the ICC
     *
     * @return String the APDU command GET CHALLENGE
     */
    public static String getChallenge() {
        return "00 84 00 00 00";
    }

    /**
     * The VERIFY command is used for OFFLINE authentication.
     * The Transaction PIN Data (input) is compared with the Reference PIN Data
     * stored in the application (ICC).
     *
     * NOTE: The EMV command "Offline PIN" is vulnerable to a Man-in-the-middle attack.
     * Terminals should request online pin verification instead!!
     *
     * TODO:
     * Plaintext PIN has been tested and verified OK. Enciphered PIN not implemented
     *
     * @param pin the PIN to verify
     * @param transmitInPlaintext 
     * @return
     */
    public static String verifyPIN(long pin, boolean transmitInPlaintext) {
        String pinStr = String.valueOf(pin);
        int pinLength = pinStr.length();
        if (pinLength < 4 || pinLength > 12) { //0x0C
            throw new SmartCardException("Invalid PIN length. Must be in the range 4 to 12. Length=" + pinLength);
        }
        StringBuilder builder = new StringBuilder("00 20 00 ");

        //EMV book 3 Table 23 (page 88) lists 7 qualifiers, 
        //but only 2 are relevant in our case (hence the use of boolean)
        byte p2QualifierPlaintextPIN = (byte) 0x80;
        byte p2QualifierEncipheredPIN = (byte) 0x88;
        if (transmitInPlaintext) {
            builder.append(Util.byte2Hex(p2QualifierPlaintextPIN));
            byte[] tmp = new byte[8]; //Plaintext Offline PIN Block. This block is split into nibbles (4 bits)
            tmp[0] = (byte) 0x20; //Control field (binary 0010xxxx)
            tmp[0] |= pinLength;
            Arrays.fill(tmp, 1, tmp.length, (byte) 0xFF); //Filler bytes

            boolean highNibble = true; //Alternate between high and low nibble
            for (int i = 0; i < pinLength; i++) { //Put each PIN digit into its own nibble
                int pos = i / 2;
                int digit = Integer.parseInt(pinStr.substring(i, i + 1)); //Safe to use parseInt here, since the original String data came from a 'long'
                if (highNibble) {
                    tmp[1 + pos] &= (byte) 0x0F; //Clear bits
                    tmp[1 + pos] |= (byte) (digit << 4);

                } else {
                    tmp[1 + pos] &= (byte) 0xF0; //Clear bits
                    tmp[1 + pos] |= (byte) (digit);
                }
                highNibble = !highNibble;
            }
            builder.append(" 08 "); //Lc length
            builder.append(Util.prettyPrintHexNoWrap(tmp)); //block


        } else {
            builder.append(Util.byte2Hex(p2QualifierEncipheredPIN));
            //Encipher PIN
            //TODO
            throw new UnsupportedOperationException("Enciphered PIN not implemented");
        }



        return builder.toString();
    }

//    //Example
//    public static String applicationBlock(int data){
//        return "8x 1E";
//    }
    public static void main(String[] args) {
        System.out.println(verifyPIN(1234, true));
    }
}
