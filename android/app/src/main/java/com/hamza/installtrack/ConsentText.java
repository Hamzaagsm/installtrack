package com.hamza.installtrack;

/**
 * Sale-agreement text — InstallTrack v2.0.
 *
 * Yehi text teen jagah istemal hota hai (ek jaisa rahe, is liye yahan
 * single source hai):
 *  1. App ki first-run consent screen (SetupActivity)
 *  2. CONSENT_AGREEMENT.md (print karke sign karwane ke liye)
 *  3. README.md
 *
 * Version badle to CONSENT_VERSION bhi badhao taake purani consent
 * naye text par dobara li jaye.
 */
public class ConsentText {

    public static final String CONSENT_VERSION = "2.0";

    public static final String AGREEMENT_URDU =
            "=== QIST PLAN — SALE AGREEMENT (InstallTrack v2.0) ===\n\n" +
            "1) Is phone par InstallTrack app lagi hui hai. Ye app seller " +
            "(Hamza Mobile) ki taraf se qist (installment) plan ke doran " +
            "phone ki hifazat ke liye lagai gayi hai.\n\n" +
            "2) YE APP KYA KARTI HAI:\n" +
            "   - Phone ki location seller ko bhejti hai (har 15 minute).\n" +
            "   - Qist ada na hone par seller phone ko LOCK kar sakta hai aur " +
            "     screen par pegham dikha sakta hai.\n" +
            "   - Phone par ek hamesha nazar aane wala notification rehta hai: " +
            "     \"Is phone par InstallTrack management active hai\".\n" +
            "   - App ka icon home screen par nazar aata hai — app chhupi " +
            "     hui NAHI hai.\n\n" +
            "3) DEVICE OWNER MANAGEMENT:\n" +
            "   - Ye phone Device Owner (fully managed) mode me hai. Is ka " +
            "     matlab hai: Settings se factory reset aur app uninstall " +
            "     qist mukammal hone tak BLOCK hai.\n" +
            "   - Phone baqi poora usable hai: calls, WiFi, Play Store, " +
            "     roz ke settings — in me se kuch block NAHI hai.\n\n" +
            "4) HATANA (REMOVAL):\n" +
            "   - Qist poori hote hi seller management HATA DEGA — phir app " +
            "     normal tarike se uninstall ho sakegi.\n" +
            "   - Buyer khud Settings se isay qist se pehle remove NAHI kar " +
            "     sakta (yehi agreement hai).\n\n" +
            "5) HONEST LIMITS:\n" +
            "   - Recovery-mode factory reset ya genuine stock firmware " +
            "     flashing se ye management mit sakta hai. Koi bhi software " +
            "     is se bach nahi sakta.\n" +
            "   - Seller qist poori hone ke baad management hatane ka " +
            "     paband hai.\n\n" +
            "Buyer ka naam: ______________  Dastakhat: ______________\n" +
            "Seller ka naam: ______________  Dastakhat: ______________\n" +
            "Tareekh: ______________  Phone model/IMEI: ______________\n";

    public static final String AGREEMENT_ENGLISH =
            "=== INSTALLMENT PLAN — SALE AGREEMENT (InstallTrack v2.0) ===\n\n" +
            "1) This phone has the InstallTrack app installed by the seller " +
            "(Hamza Mobile) to protect the phone during the installment " +
            "(qist) plan.\n\n" +
            "2) WHAT THIS APP DOES:\n" +
            "   - Sends the phone's location to the seller (every 15 minutes).\n" +
            "   - If an installment is not paid, the seller can LOCK the phone " +
            "     and show a message on the screen.\n" +
            "   - A permanent notification stays visible: " +
            "\"InstallTrack management is active on this phone\".\n" +
            "   - The app icon stays visible on the home screen — the app " +
            "     is NOT hidden.\n\n" +
            "3) DEVICE OWNER MANAGEMENT:\n" +
            "   - This phone is in Device Owner (fully managed) mode. That " +
            "     means factory reset from Settings and app uninstall are " +
            "     BLOCKED until the installments are complete.\n" +
            "   - The phone stays fully usable otherwise: calls, WiFi, " +
            "     Play Store, daily settings — none of these are blocked.\n\n" +
            "4) REMOVAL:\n" +
            "   - When the installments are complete, the seller WILL remove " +
            "     the management — after that the app can be uninstalled " +
            "     normally.\n" +
            "   - The buyer cannot remove it from Settings before the " +
            "     installments are complete (that is the agreement).\n\n" +
            "5) HONEST LIMITS:\n" +
            "   - A recovery-mode factory reset or flashing genuine stock " +
            "     firmware can remove this management. No software can " +
            "     prevent that.\n" +
            "   - The seller is obliged to remove the management once the " +
            "     installments are complete.\n\n" +
            "Buyer name: ______________  Signature: ______________\n" +
            "Seller name: ______________  Signature: ______________\n" +
            "Date: ______________  Phone model/IMEI: ______________\n";
}
