package ca.uwaterloo.crysp.privacyguard.Plugin;

import android.util.Log;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.whois.WhoisClient;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import ca.uwaterloo.crysp.privacyguard.Application.Database.DatabaseHandler;


/**
 * Class is largely based on  gibberish detector used to train and classify sentences as gibberish or not.
 *
 * @author sfiszman (https://github.com/paypal/Gibberish-Detector-Java)
 */
public class DGADetector {
    private final Map<Character, Integer> alphabetPositionMap = new HashMap<>();
    private static final int MIN_COUNT_VAL = 10;
    private DatabaseHandler db;
    private HashMap<String, Result> cache;
    private final String alphabet = "abcdefghijklmnopqrstuvwxyz ";
    private final double[][] logProbabilityMatrix = {
            {-8.569134847000806, -3.9369307938330933, -3.2206676967672974, -3.048245521037517, -6.052276597406203, -4.699558531819917, -3.994156130878688, -6.710404751666567, -3.245301640130125, -7.060737789080015, -4.512280893694204, -2.4997176870344004, -3.6426343157108727, -1.5707438146424084, -7.978466335723797, -3.893639344291984, -9.821897815496174, -2.3025259123500446, -2.348363959452305, -1.944862676264688, -4.539155660733608, -3.8718472941849895, -4.706356654533738, -6.560310872534923, -3.6497228572775398, -6.64195183699619, -2.7135048994495476},
            {-2.5528619980785, -5.139226208055755, -6.049719822245583, -6.219404795035026, -1.173596307609444, -8.563954087128105, -8.805116143944993, -8.494961215641153, -3.3328454702735173, -5.004532700251055, -8.805116143944993, -2.139085063222716, -6.121607051758899, -6.808562262070924, -2.1459387811703974, -8.312639658847198, -8.900426323749317, -2.719375008855968, -3.788438535392774, -4.703224376087508, -2.137350056136624, -6.320209494156992, -7.676650892127201, -8.900426323749317, -2.3611294272462486, -8.900426323749317, -4.738423113053401},
            {-2.089456234355858, -9.398278073111246, -3.846611313191934, -7.6784921035082805, -1.7391067054451883, -8.79214226954093, -9.580599629905201, -1.909331907702555, -2.93317069336499, -9.485289450100876, -3.3436431278192473, -3.2765165250554067, -8.515888892912773, -8.838662285175824, -1.6000286172989644, -9.485289450100876, -6.386016497606045, -3.3776593363506913, -5.838179408863235, -2.390903729100397, -3.2184339671253945, -9.485289450100876, -8.838662285175824, -9.580599629905201, -4.621257630196496, -8.299665784443137, -3.8671969118720093},
            {-3.720003681012089, -7.418390640747022, -7.886769574265756, -4.564821564257741, -1.969902629650379, -6.7968529925652, -5.43040492781724, -6.754111444187929, -2.46267809788966, -6.2773316618316555, -7.666226804651603, -4.558282540490684, -5.51883460106326, -5.946440835048637, -3.116965813103029, -8.38042739440938, -8.136230433897339, -3.624272629631426, -3.6811082441529064, -7.193622393705811, -3.994154468854048, -5.568720255574128, -7.077283401621661, -9.927989903125393, -4.613799187421067, -9.745668346331438, -0.539478241478342},
            {-3.095660130809256, -6.327925368138923, -3.7113631326589287, -2.414278111744593, -3.72797916864029, -4.5550648250789045, -4.958205972113245, -6.340793192750316, -4.465084899593527, -8.043231281651536, -7.012886964112429, -3.4395342614577955, -3.748954644626845, -2.3891226101897263, -5.281167293781721, -4.426567799165937, -6.265100643479086, -1.9762402620482646, -2.5191950026773466, -3.7330796591427693, -6.051262690936753, -4.115195043170505, -4.719218316325699, -4.45114788228114, -4.53555725616536, -7.731723630787685, -1.129201313526555},
            {-2.7634422911455703, -7.9114774546442685, -7.529542843946299, -8.494623739989885, -2.451100566435661, -2.926120215794328, -7.612234559791412, -8.53718335440868, -2.4505074466080714, -9.033620240722573, -8.72823859117139, -3.748243385450271, -8.839464226281615, -7.66534438510536, -1.9043799278633868, -8.305381740351358, -9.370092477343785, -2.3558173549839756, -5.929674382528349, -3.315653131074415, -3.5058932812816797, -9.370092477343785, -7.801476559429941, -9.370092477343785, -6.135343303319295, -9.370092477343785, -0.9976707550554195},
            {-2.6810747465294993, -8.560210108663561, -8.083286036573252, -6.88623367509189, -1.9631402016759496, -7.924221341943565, -4.619707631913547, -2.2213216097310324, -2.86125407738429, -9.14799677356568, -8.560210108663561, -3.358036602668427, -6.0753034588755614, -3.7399285430585247, -2.831915706212855, -7.954074305093246, -9.052686593761356, -2.552626179675696, -4.0628726274786855, -4.943304154174714, -3.494455515346226, -9.14799677356568, -7.579380855651835, -9.14799677356568, -5.882237362798629, -8.61736852250351, -1.0302965589873185},
            {-1.894983266008102, -7.387203121199488, -8.053100659312058, -7.5422750355460675, -0.7286319445696532, -7.8583123337529734, -9.589967878911324, -8.779037662694995, -1.9951337947708476, -10.100793502677314, -7.885219786672898, -6.635057599877587, -6.2613411900840035, -6.705167166064614, -2.5617664468533192, -9.25349564229011, -10.100793502677314, -4.572026009632629, -6.222672048924849, -3.7645579161334237, -4.622240085826344, -9.541177714741892, -7.381693465388519, -10.28311505947127, -5.024578562935076, -10.187804879666944, -2.362341691662547},
            {-3.712244886548001, -4.717475282130211, -2.783984370515817, -3.2216013768067646, -3.1683365236496233, -3.90382545586124, -3.680790547058952, -9.119304544100272, -6.422989599216483, -10.323277348426208, -5.240838322200969, -3.0795261367137394, -3.173687619686372, -1.3126793991869439, -2.664381175855551, -4.9236074876178835, -7.895529112478156, -3.409705684122631, -2.051579832828198, -2.1011937452905483, -6.511074678280273, -3.8167461832949807, -9.672689782285058, -6.168308164387673, -10.410288725415837, -5.511770729440288, -3.788398987959084},
            {-2.3427609160575655, -6.1024094410597085, -6.037870919922137, -6.507874549167873, -1.4153520955994334, -6.245510284700382, -6.17140231254666, -6.325552992373918, -5.51462277615759, -6.507874549167873, -6.245510284700382, -6.412564369363548, -6.325552992373918, -6.507874549167873, -1.2783714986201964, -6.412564369363548, -6.325552992373918, -6.1024094410597085, -6.1024094410597085, -6.245510284700382, -1.1002544477293865, -6.507874549167873, -6.245510284700382, -6.507874549167873, -6.507874549167873, -6.507874549167873, -4.859215923580492},
            {-3.6194623078975634, -7.047672488805787, -6.062388885444681, -7.671826797878781, -1.2114008311035072, -6.28553243675889, -6.824528937491578, -3.631117451489312, -1.7851673651369253, -7.814927641519454, -7.489505241084826, -3.88763716396052, -6.0360715771273075, -2.3543606124919836, -3.8417371752133214, -7.546663654924775, -7.981981726182621, -5.48203719903008, -3.0008688713526865, -6.690997544867055, -3.7386948292403996, -7.244382783051841, -5.438234576371687, -8.077291905986945, -4.682783512475587, -8.077291905986945, -1.4289552341837297},
            {-2.269173189852316, -6.5732876012327095, -5.745469166987859, -2.8596265230013502, -1.7841245065459865, -4.1440377024475, -6.844081455655969, -7.743764451130463, -2.175105672555906, -9.70150905783278, -4.95657692946953, -2.065923407729852, -5.060328434321655, -6.49268356881808, -2.4508735459340993, -5.604390568727954, -9.70150905783278, -5.64827588385311, -3.879449842252206, -3.8645077393903557, -3.8209760714320793, -5.091351330333649, -5.466195552485485, -9.883830614626735, -2.33359006988688, -8.60289676916467, -2.043478277496132},
            {-1.7539862326257705, -3.6841118931855092, -6.559303093904658, -8.439615960474159, -1.3654364247584612, -6.407576657688907, -9.337557553680117, -8.238945265012006, -2.438934758261252, -9.17050346901695, -8.90223948242227, -6.206023738967064, -3.6410743815055273, -5.662408292378083, -2.2280495202447717, -2.697206491140823, -9.432867733484441, -5.501042100760116, -3.5381894550641513, -6.907139089176186, -3.4685174788680313, -9.250546176690486, -8.07189118034884, -9.432867733484441, -3.4247919205712627, -9.432867733484441, -1.8968771981195023},
            {-3.4115438953131507, -6.728251969418387, -3.09090950410042, -1.7402315867181457, -2.5014858122682413, -4.81733866240017, -2.1131772915448415, -6.807716140772634, -3.2957593101225076, -6.2527657120977045, -4.917764645365365, -4.63273660290749, -5.988742613962571, -4.666618517669701, -2.8767262790957036, -7.603720706772286, -6.8644323702144865, -7.187205762477537, -3.0523029767834418, -2.264714687927007, -4.900124856019456, -5.34892627761459, -7.147703319501291, -7.4543433056976856, -4.546318606185202, -8.807693511098222, -1.4657896898065892},
            {-5.082238782674552, -5.142391025529309, -4.276595746864632, -4.0852142112255745, -5.759965778546622, -2.174771009531072, -5.288618965754418, -6.0901715038960225, -4.471569724355131, -6.906872076573688, -4.432656048947973, -3.2277404998496717, -2.821289749187092, -1.7681062329070991, -3.5156198174449305, -3.951799050367325, -8.986313618253524, -2.1664564729499034, -3.3712105776876493, -3.12244572825025, -2.210186781749823, -3.5450690342480784, -3.1404771440030212, -6.663109238056743, -5.513441778588349, -7.704223034663635, -2.2129949033447684},
            {-2.136245066925728, -7.648729647534111, -6.522143506823595, -8.503144975690178, -1.734594294428591, -6.501664975480054, -7.785305182539862, -3.6425576778375817, -2.654252565858868, -8.454354811520746, -7.861291089517784, -2.385157915872412, -6.3630788121939075, -7.294184629853203, -2.1161814847858818, -2.790733174335923, -9.196292156250124, -1.7868529114192793, -3.848708548399169, -3.294205420293358, -3.1971074861270035, -9.196292156250124, -7.080036641447571, -9.196292156250124, -4.987131919599442, -9.196292156250124, -2.936710692185201},
            {-6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -0.057170565024993084, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -6.182291496945648, -5.540437610773254},
            {-2.572261043912993, -5.992037505465634, -4.319297013220492, -3.728409655724781, -1.4225286304249027, -5.357211935340673, -4.331741175798042, -6.033738234664577, -2.3645219944883915, -9.405163457993062, -4.85044481550107, -4.673536848052412, -3.7449846208189936, -3.9165563650018944, -2.3221448060288776, -5.413959654690475, -9.040520344405154, -3.6368424621992905, -2.9015941056207097, -3.234549045256761, -3.9816241266916443, -4.9057983309019475, -6.257568835129826, -10.139132633073263, -3.2585772740177803, -8.070162391260723, -1.7282263121749826},
            {-3.2089376903705964, -6.334825245044646, -4.093224786307717, -7.591960760952624, -2.1585602921938984, -6.182920915843763, -7.849429054807908, -2.9151835364794887, -2.775396763880981, -9.550216745830236, -4.550950179340741, -4.7070572326766555, -4.5891980442577935, -6.243170795291188, -2.9688542376053113, -3.853853026561481, -7.008957159491104, -8.24305970526907, -2.8107772166413194, -2.1176814856736472, -3.2564760527584085, -7.865429396154349, -5.380522746366349, -10.38312586876534, -5.201342318473255, -9.913122239519605, -0.9912559201954122},
            {-3.168250714968673, -8.21768628916598, -5.8788799327376156, -9.525199772432757, -2.338871969045315, -7.172382553952377, -8.478412551629232, -1.107451965673994, -2.3662142437968092, -10.4567579764377, -9.689502823724032, -4.418519949316138, -6.032372067924676, -7.2195889585221735, -2.33669549018575, -8.161894929537564, -10.71912224090519, -3.4624016610543737, -3.679724852871338, -4.025179185808379, -3.900198175629669, -9.55597143109951, -5.1825756856249034, -10.131335576003071, -4.204705890233377, -7.984754731485607, -1.5836024025866968},
            {-3.6907063474960813, -3.7341336409281, -3.2454721262149633, -4.020608712629845, -3.281297978688059, -5.0172437658127595, -3.1933271849501734, -7.7963898885987, -3.73111886626001, -9.35453450664525, -6.208229374611885, -2.257675168572427, -3.4112977818505237, -2.089862646254399, -6.096437968623768, -3.100705695069777, -8.949069398537086, -1.905375903432984, -1.9722545836234537, -1.9648676140617645, -9.274491798971713, -6.7831953510849425, -9.35453450664525, -7.31765257938421, -7.3735330377786665, -5.267158613739243, -3.2700350935700784},
            {-2.4677551327310105, -8.46601472297182, -8.561324902776146, -7.868177722216201, -0.5192363285514866, -8.561324902776146, -8.298960638308655, -8.46601472297182, -1.7429474349452256, -8.46601472297182, -7.919471016603751, -5.443374996497906, -8.561324902776146, -4.566800675836256, -2.7892609207035393, -8.561324902776146, -8.561324902776146, -6.268790145635601, -4.407140340198028, -7.973538237874027, -6.289199017266808, -8.379003345982191, -8.030696651713976, -8.46601472297182, -5.258107929474194, -8.561324902776146, -3.1228109057348257},
            {-1.596798460957614, -7.6001421705956735, -7.640964165115928, -5.344648685135478, -1.8921021056868312, -6.839336341561913, -8.110967794361665, -1.6228272590921338, -1.7628783045649916, -8.87310784640856, -7.001305669506969, -5.467159861987808, -8.179960665848615, -3.2183655671770013, -2.524218636471301, -8.293289351155618, -9.209580083029774, -4.57000847032435, -4.2425484264156506, -5.65423202154036, -7.560921457442392, -9.209580083029774, -7.307472556632853, -9.209580083029774, -6.877436187794184, -9.02725852623582, -2.172728230713227},
            {-2.25468000382515, -6.898108901930332, -2.027502252437779, -6.492643793822167, -2.4625415003284203, -5.833398164937903, -6.898108901930332, -4.310344866702623, -2.073000295576979, -6.898108901930332, -6.898108901930332, -6.428105272684596, -6.715787345136377, -6.898108901930332, -4.455761866561128, -1.4954315200580521, -6.367480650868162, -4.636345803456541, -5.766706790439231, -1.870288783079975, -4.372380257622076, -4.34866373100476, -6.802798722126007, -4.473306176212037, -5.175342304189228, -6.898108901930332, -2.556904261776705},
            {-3.8727557689577803, -6.0797197529850235, -5.605837644410717, -6.149453090999699, -2.917990150626026, -5.833021605261932, -6.998604520036225, -7.3170582511547595, -3.826520856674783, -8.857503292101908, -7.450589643779282, -4.745264240003257, -4.447739902456427, -5.448007107625058, -2.225391727145099, -4.737112020941707, -9.039824848895863, -5.751422961379052, -3.132557460588637, -4.230082497178997, -7.038344848685739, -7.6535304877759724, -6.2304221535333655, -7.704823782163523, -8.164356111541963, -7.731492029245684, -0.3818441542153365},
            {-2.531179599331457, -6.00314605188182, -5.907835872077495, -4.694813232231641, -0.9292230185496455, -5.907835872077495, -6.00314605188182, -3.199785670975285, -2.3395844057521735, -6.00314605188182, -5.666673815260607, -3.886890537079268, -4.0016660516716955, -5.009894278871537, -1.7362497244615696, -6.00314605188182, -6.00314605188182, -6.00314605188182, -5.820824495087865, -5.907835872077495, -3.1183453390351104, -5.666673815260607, -5.907835872077495, -6.00314605188182, -4.568061526592497, -3.7731316517226094, -3.1699327078256037},
            {-2.1391027232585453, -3.120839066755946, -3.2055065209985045, -3.552215094494913, -3.8301145361996793, -3.252413323847759, -4.127115601441595, -2.7683496198244715, -2.739305380929777, -5.683438404671536, -5.260781996831157, -3.773583808395596, -3.343713764728033, -3.800100843982604, -2.6282891376899533, -3.36662774608745, -6.233077965863898, -3.679698888825022, -2.6975774183060213, -1.8443705253486264, -4.4631221175357245, -4.919749615503536, -2.7878010613137674, -7.815688636064855, -4.6839311457777, -8.475097676992949, -3.6438309580642136}
    };
    private final double threshold = 0.02393848376199023;
    private static DGADetector instance;

    private DGADetector(DatabaseHandler db) {
        initializePositionMap();
        this.db = db;
        cache = db.getDomainCache();
    }

    public static DGADetector getInstance(DatabaseHandler db) {
        if (instance == null) {
            instance = new DGADetector(db);
        }
        return instance;
    }

    // can be overridden for another threshold heuristic implementation
    protected double getThreshold(double minGood, double maxBad) {
        return (minGood + maxBad) / 2;
    }

    private synchronized void updateCache(String domain, Result result) {
        cache.put(domain, result);
        db.addDomainCache(domain, result);
    }

    private void initializePositionMap() {

        char[] alphabetChars = alphabet.toCharArray();
        for (int i = 0; i < alphabetChars.length; i++) {
            alphabetPositionMap.put(alphabetChars[i], i);
        }
    }

    private String normalize(String line) {
        StringBuilder normalizedLine = new StringBuilder();
        for (char c : line.toLowerCase().toCharArray()) {
            normalizedLine.append(alphabet.contains(Character.toString(c)) ? c : "");
        }
        return normalizedLine.toString();
    }

    private List<String> getNGram(int n, String line) {
        String filteredLine = normalize(line);
        List<String> nGram = new ArrayList<String>();
        for (int start = 0; start < filteredLine.length() - n + 1; start++) {
            nGram.add(filteredLine.substring(start, start + n));
        }
        return nGram;
    }

/*    private int[][] getAlphaBetCouplesMatrix(List<String> trainingLinesList) {
        int[][] counts = createArray(alphabet.length());
        for (String line : trainingLinesList) {
            List<String> nGram = getNGram(2, line);
            for (String touple : nGram) {
                counts[alphabetPositionMap.get(touple.charAt(0))][alphabetPositionMap.get(touple.charAt(1))]++;
            }
        }
        return counts;
    }

    private double[][] getLogProbabilityMatrix(int[][] alphabetCouplesMatrix) {
        int alphabetLength = alphabet.length();
        double[][] logProbabilityMatrix = new double[alphabetLength][alphabetLength];
        for (int i = 0; i < alphabetCouplesMatrix.length; i++) {
            double sum = getSum(alphabetCouplesMatrix[i]);
            for (int j = 0; j < alphabetCouplesMatrix[i].length; j++) {
                logProbabilityMatrix[i][j] = Math.log(alphabetCouplesMatrix[i][j] / sum);
            }
        }
        return logProbabilityMatrix;
    }

    private List<Double> getAvgTransitionProbability(List<String> lines, double[][] logProbabilityMatrix) {
        List<Double> result = new ArrayList<Double>();
        for (String line : lines) {
            result.add(getAvgTransitionProbability(line, logProbabilityMatrix));
        }
        return result;
    }*/

    private double getAvgTransitionProbability(String line, double[][] logProbabilityMatrix) {
        double logProb = 0d;
        int transitionCount = 0;
        List<String> nGram = getNGram(2, line);
        for (String touple : nGram) {
            logProb += logProbabilityMatrix[alphabetPositionMap.get(touple.charAt(0))][alphabetPositionMap.get(touple.charAt(1))];
            transitionCount++;
        }
        return Math.exp(logProb / Math.max(transitionCount, 1));
    }

    /*private int[][] createArray(int length) {
        int[][] counts = new int[length][length];
        for (int i = 0; i < counts.length; i++) {
            Arrays.fill(counts[i], MIN_COUNT_VAL);
        }
        return counts;
    }

    private double getSum(int[] array) {
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }*/

    /**
     * determines if a sentence is gibberish or not.
     *
     * @param line a sentence to be classified as gibberish or not.
     * @return true if the sentence is gibberish, false otherwise.
     */

    private boolean isDGA(String line) {

        boolean retval = false;
        String domain;
        String[] temp = line.split("\\.");
        if (temp.length < 3) {
            domain = temp[0];
        } else {
            domain = temp[1];
        }
        Log.d("Domain", domain);
        if (domain.length() > 5) {
            Log.d("Domain", Double.toString(getAvgTransitionProbability(domain, logProbabilityMatrix)));
            if (getAvgTransitionProbability(domain, logProbabilityMatrix) < threshold) {
                retval = true;
            }
        }
        return retval;
    }

    public double getScoreThresh() {
        return 0;
    }

    public Result getResult(String domain) {
        String TAG = "getScore";
        int domain_score;
        int cYear = Calendar.getInstance().get(Calendar.YEAR);
        int dYear = cYear;
        String sDomain = getDom(domain);

        // check for cache if found return cache
        Result ret = cache.get(sDomain);
        if (ret != null) {
            return ret;
        }

        ret = new Result();
        ret.score = 0;
        ret.isDGA = false;

        //query if its bad domain, if yes +100
        //Sensitive NameServer if matches dynDNS, +15
        //ASN if needed, if detect adobe and ASN does not correspond to adobe +30
        asyncAPI query = new asyncAPI();
        try {
            int res = query.execute(sDomain).get();
            ret.score += res;
            if (res == 100){
                return ret;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isDGA(sDomain)) {
            //domain_score = 75;
            ret.score+=75;
            ret.isDGA = true;
        }
        AsyncWHOIS task = new AsyncWHOIS();
        try {
            dYear = task.execute(sDomain).get();
            if (dYear == 0) {
                dYear = cYear;
            }
            Log.d(TAG, Integer.toString(dYear));
        } catch (Exception e) {
            e.printStackTrace();
        }
         //WHOIS date range 0 - 1 year "0", 1-3 years "-10", 3-5 years "-20", >5 years "-30"
        if (((cYear - dYear) > 1) && ((cYear - dYear) <= 3)) {
            ret.score -= 20;
        }else if (((cYear - dYear) > 3) && ((cYear - dYear) <= 5)) {
            ret.score -= 50;
        } else if ((cYear - dYear) > 5) {
            ret.score -= 70;
        }

        // update cache
        updateCache(sDomain, ret);

        return ret;
    }

    private String getDom(String domain) {
        String TAG = "getDom";
        String[] ccTLDs = {"ac", "ad", "ae", "af", "ag", "ai", "al", "am", "ao", "aq", "ar", "as", "at", "au", "aw", "ax", "az", "ba", "bb", "bd", "be", "bf", "bg", "bh", "bi", "bj", "bm", "bn", "bo", "br", "bs", "bt", "bw", "by", "bz", "ca", "cc", "cd", "cf", "cg", "ch", "ci", "ck", "cl", "cm", "cn", "co", "cr", "cu", "cv", "cw", "cx", "cy", "cz", "de", "dj", "dk", "dm", "do", "dz", "ec", "ee", "eg", "er", "es", "et", "eu", "fi", "fj", "fk", "fm", "fo", "fr", "ga", "gd", "ge", "gf", "gg", "gh", "gi", "gl", "gm", "gn", "gp", "gq", "gr", "gs", "gt", "gu", "gw", "gy", "hk", "hm", "hn", "hr", "ht", "hu", "id", "ie", "il", "im", "in", "io", "iq", "ir", "is", "it", "je", "jm", "jo", "jp", "ke", "kg", "kh", "ki", "km", "kn", "kp", "kr", "kw", "ky", "kz", "la", "lb", "lc", "li", "lk", "lr", "ls", "lt", "lu", "lv", "ly", "ma", "mc", "md", "me", "mg", "mh", "mk", "ml", "mm", "mn", "mo", "mp", "mq", "mr", "ms", "mt", "mu", "mv", "mw", "mx", "my", "mz", "na", "nc", "ne", "nf", "ng", "ni", "nl", "no", "np", "nr", "nu", "nz", "om", "pa", "pe", "pf", "pg", "ph", "pk", "pl", "pm", "pn", "pr", "ps", "pt", "pw", "py", "qa", "re", "ro", "rs", "ru", "rw", "sa", "sb", "sc", "sd", "se", "sg", "sh", "si", "sk", "sl", "sm", "sn", "so", "sr", "ss", "st", "sv", "sx", "sy", "sz", "tc", "td", "tf", "tg", "th", "tj", "tk", "tl", "tm", "tn", "to", "tr", "tt", "tv", "tw", "tz", "ua", "ug", "uk", "us", "uy", "uz", "va", "vc", "ve", "vg", "vi", "vn", "vu", "wf", "ws", "ye", "yt", "za", "zm", "zw"};
        String[] TLDs = {"com", "net", "org", "info", "biz", "io", "edu", "gov"};

        String[] d = domain.split("\\.");
        StringBuilder sbDomain = new StringBuilder();
        if (d.length > 2) {
            List<String> clist = Arrays.asList(ccTLDs);
            List<String> tlist = Arrays.asList(TLDs);
            if ((clist.contains(d[d.length - 1])) && ((tlist.contains(d[d.length - 2])))) {
                // last entry is ccTLD && 2nd last is a valid TLD
                sbDomain.append(d[d.length - 3]);
                sbDomain.append(".");
                sbDomain.append(d[d.length - 2]);
                sbDomain.append(".");
                sbDomain.append(d[d.length - 1]);
                // last entry is ccTLD && 2nd last is not a valid TLD
            } else if ((clist.contains(d[d.length - 1])) && (!(tlist.contains(d[d.length - 2])))) {
                sbDomain.append(d[d.length - 2]);
                sbDomain.append(".");
                sbDomain.append(d[d.length - 1]);
            } else {
                // last entry is not ccTLD
                sbDomain.append(d[d.length - 2]);
                sbDomain.append(".");
                sbDomain.append(d[d.length - 1]);
            }
        } else {
            sbDomain.append(d[0]);
            sbDomain.append(".");
            sbDomain.append(d[1]);
        }
        Log.d(TAG, sbDomain.toString());
        return sbDomain.toString();
    }

    private static class AsyncWHOIS extends AsyncTask<String, Void, Integer> {
        String TAG = "AsyncWHOIS";

        private int GetCreationDate(String domain) {
            String TAG = "getCreationDate";
            String next_hop;
            String[] tempbuf;

            Log.d(TAG, "Entered Function getCreationDate");
            WhoisClient whoisClient = new WhoisClient();
            int index;
            try {
                whoisClient.connect("whois.iana.org", WhoisClient.DEFAULT_PORT);
                String result = whoisClient.query(domain);
                Log.d(TAG,"WHOIS result from iana.org" + result);
                while (true) {
                    index = result.indexOf("refer:");
                    if (index != -1) {
                        String[] refer = result.substring(index).split("\\n");
                        String[] finalR = refer[0].split(" ");
                        next_hop = finalR[finalR.length - 1];
                        Log.d(TAG, "Next WHOIS server to query " + next_hop);
                        whoisClient.connect(next_hop, WhoisClient.DEFAULT_PORT);
                        result = whoisClient.query(domain);
                    } else {
                        break;
                    }
                }
                Log.d(TAG,"Final WHOIS result: "+ result);
                index = result.indexOf("Creation Date:");
                if (index == -1) {
                    index = result.indexOf("created:");
                }
                if (index != -1) {
                    tempbuf = result.substring(index).split("\\n");
                    Pattern pattern = Pattern.compile("(19|20)\\d{2}");
                    Matcher matcher = pattern.matcher(tempbuf[0]);
                    String cYear = "";
                    if (matcher.find()) {
                        cYear = matcher.group(0);
                    }
                    return Integer.parseInt(cYear);
                } else {
                    return 0;
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception occurred in getCreationDate");
                Log.d(TAG, e.getCause() + " " + e.getMessage());
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        protected Integer doInBackground(String... params) {
            Log.i(TAG, "DomDetector - doInBackground");
            int domainYear;
            Log.d(TAG, params[0]);

            domainYear = GetCreationDate(params[0]);
            Log.d(TAG, Integer.toString(domainYear));
            return domainYear;
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log.i(TAG, "onPostExecute " + result.toString());
        }
    }

    private static class asyncAPI extends AsyncTask<String, Void, Integer> {
        String TAG = "asyncAPI";

        private String getASN(String domainIP){
            String TAG="asyncAPI - getASN";
            try {
                String addr = "https://api.apility.net/v2.0/as/ip/" + domainIP;
                URL dstURL = new URL(addr);
                String input;

                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) dstURL.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);

                https.setRequestMethod("GET");
                https.setRequestProperty("X-Auth-Token", "aae0d598-e46a-49e7-aab1-05195d8cddd3");
                https.setRequestProperty("Accept", "application/json");

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(https.getInputStream()));

                input = br.readLine();
                br.close();
                Log.d(TAG, "API return JSON: "+ input);
                JSONObject mainObject = new JSONObject(input);
                JSONObject asObj = mainObject.getJSONObject("as");
                Log.d(TAG, "API return JSON: "+ asObj.toString());
                return asObj.getString("asn");
            }
            catch(Exception e){
                Log.d(TAG,"Error in getASN()");
                Log.d(TAG,e.getCause()+" "+e.getMessage());
                return "";
            }

        }
        @Override
        protected Integer doInBackground(String... params) {
            String TAG ="asyncAPI - Background";
            Log.d(TAG, "Looking up domain: " + params[0]);
            String domain = params[0].toLowerCase();
            int dScore =0;
            String addr = "https://api.apility.net/baddomain/"+domain;
            String input;
            String[] googleASNs = {"36040", "45566", "41264", "36384", "22577", "36492", "15169"};
            String[] adobeASNs = {"44786", "22786", "19238", "1313"};
            String[] microsoftASNs = {"26222", "3598", "6182", "8068", "8075", "20046", "8072", "23468", "13811", "8069"};
            String[] chaseASNs;
            String[] wellsfargoASNs = {"4196", "10837"};
            List<String> tempList;

            try {
                URL dstURL = new URL(addr);
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) dstURL.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);

                https.setRequestMethod("GET");
                https.setRequestProperty("X-Auth-Token", "aae0d598-e46a-49e7-aab1-05195d8cddd3");
                https.setRequestProperty("Accept", "application/json");

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(https.getInputStream()));

                input = br.readLine();
                br.close();

                JSONObject mainObject = new JSONObject(input);
                JSONObject respObj = mainObject.getJSONObject("response");
                JSONObject domObj = respObj.getJSONObject("domain");
                dScore = Integer.parseInt(domObj.getString("score"));
                if (dScore == -1) {
                    dScore = 100;
                }else {
                    JSONArray nameServers = domObj.optJSONArray("ns");
                    for (int i = 0; i < nameServers.length(); i++) {
                        if ((nameServers.get(i).toString().toLowerCase().contains("no-ip.org")) ||
                                (nameServers.get(i).toString().toLowerCase().contains("dyndns.org"))) {

                            Log.d(TAG, "Domain name server matches dynamic DNS domains: " +
                                    nameServers.get(i).toString().toLowerCase());
                            dScore += 50;
                        }
                    }
                    JSONObject ipObj = respObj.getJSONObject("ip");

                    if (domain.contains("google") || domain.contains("adobe") || domain.contains("microsoft")) {
                        Log.d(TAG, "Likely phishing domain found: " + domain);
                        String dASN = getASN(ipObj.getString("address"));
                        Log.d(TAG, "domain: " + domain + "ASN: " + dASN);
                        if (domain.contains("google")) {
                            tempList = Arrays.asList(googleASNs);
                            if (!tempList.contains(dASN)) {
                                //suspicious
                                dScore += 80;
                            }
                        } else if (domain.contains("adobe")) {
                            tempList = Arrays.asList(adobeASNs);
                            if (!tempList.contains(dASN)) {
                                //suspicious
                                dScore += 80;
                            }
                        } else if (domain.contains("microsoft")) {
                            tempList = Arrays.asList(microsoftASNs);
                            if (!tempList.contains(dASN)) {
                                //suspicious
                                dScore += 80;
                            }
                        }
                    }
                }
                return dScore;
            } catch (Exception e) {
                Log.d(TAG,"Exception occurred in asyncAPI - Background");
                Log.d(TAG, e.getCause() +" "+e.getMessage());
                //e.printStackTrace();
            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
        }

        // always verify the host - dont check for certificate
        final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        /**
         * Trust every server - dont check for any certificate
         */
        private void trustAllHosts() {
            // Create a trust manager that does not validate certificate chains
            String TAG = "trustAllHost";
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }

                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }
            }};

            // Install the all-trusting trust manager
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection
                        .setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                Log.d(TAG,"Error setting SSLSocketFactory");
            }
        }
    }

    public static class Result {
        public double score;
        public boolean isDGA;
    }
}