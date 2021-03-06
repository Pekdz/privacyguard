package ca.uwaterloo.crysp.privacyguard.Plugin;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ca.uwaterloo.crysp.privacyguard.Application.Database.DatabaseHandler;
import ca.uwaterloo.crysp.privacyguard.Application.Logger;
import ca.uwaterloo.crysp.privacyguard.Application.Network.ConnectionMetaData;
import ca.uwaterloo.crysp.privacyguard.Application.Network.L7Protocol;


public class CryptominerDetectionPlugin implements IPlugin {
    private final boolean DEBUG = true;
    private final String TAG = CryptominerDetectionPlugin.class.getSimpleName();
    private DatabaseHandler db;
    private HelperTool tool;

    @Override
    @Nullable
    public LeakReport handleRequest(String request, byte[] rawRequest, ConnectionMetaData metaData) {
        try {
            if (metaData.protocol == L7Protocol.WEBSOCKET) {
                ArrayList<LeakInstance> leaks = new ArrayList<>();

                if (metaData.currentPacket != null) {
                    String wsPayload = metaData.currentPacket.payload;

                    // check for cyptominer
                    boolean domainIsMiningPool = tool.isMiningPool(metaData.destHostName);
                    String signatureName = tool.getSignature(wsPayload);

                    if (DEBUG) {
                        Logger.i(TAG, "Cryptominer Result => isPool: " + domainIsMiningPool
                                + ", signature: " + signatureName);
                    }

                    if (domainIsMiningPool || !signatureName.equals("N/A")) {
                        // check packet record isn't saved to database
                        if (metaData.currentPacket.dbId == -1) {
                            metaData.currentPacket = db.addPacketRecord(metaData.currentPacket);
                        }

                        LeakInstance leak = new CryptominerInstance("Cyptominer detected", metaData.destHostName,
                                metaData.currentPacket.dbId, domainIsMiningPool, signatureName, metaData.currentPacket.time);
                        leaks.add(leak);
                    }
                }

                if (leaks.isEmpty())
                    return null;

                LeakReport rpt = new LeakReport(LeakReport.LeakCategory.CRYPTOMINER);
                rpt.addLeaks(leaks);
                return rpt;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public LeakReport handleResponse(String response) {
        return null;
    }

    @Override
    public String modifyRequest(String request) {
        return request;
    }

    @Override
    public String modifyResponse(String response) {
        return response;
    }

    @Override
    public void setContext(Context context) {
        db = DatabaseHandler.getInstance(context);
        tool = HelperTool.getInstance();
    }


    static class HelperTool {
        private List<String> urls = Arrays.asList("50btc.com", "abcpool.co", "alvarez.sfek.kz", "bitalo.com", "bitcoinpool.com",
                "bitminter.com", "mmpool.bitparking.com", "blisterpool.com", "btcguild.com", "btcmine.com", "btcmp.com",
                "btcmow.com", "btcwarp.com", "btcpoolman.com", "coinminers.co", "coinotron.com", "deepbit.net");
        private Set<String> MiningPoolUrls = new HashSet<>(urls);
        private static HelperTool instance;

        private HelperTool() {
        }

        public static HelperTool getInstance() {
            if (instance == null)
                instance = new HelperTool();
            return instance;
        }

        //detect url of mining pool
        public boolean isMiningPool(String domain) {
            return MiningPoolUrls.contains(domain);
        }

        public String getSignature(String payload) {
            String signature = "N/A";
            if (checkCoinHive((payload)))
                signature = "Coinhive";

            return signature;
        }

        //check json object schema
        private boolean checkJsonSchema(JSONObject obj) throws JSONException {
            Iterator<String> keys = obj.keys();
            List<String> keysList = new ArrayList<>();
            while (keys.hasNext()) {
                String key = keys.next();
                keysList.add(key);
            }
            if (keysList.size() == 2
                    && keysList.contains("type")
                    && keysList.contains("params")) {
                JSONObject sub_obj = obj.getJSONObject("params");
                Iterator<String> sub_keys = sub_obj.keys();
                List<String> sub_keysList = new ArrayList<>();
                while (sub_keys.hasNext()) {
                    String sub_key = sub_keys.next();
                    sub_keysList.add(sub_key);
                }
                if (sub_keysList.size() == 5) {
                    return sub_keysList.contains("version")
                            && sub_keysList.contains("site_key")
                            && sub_keysList.contains("type")
                            && sub_keysList.contains("user")
                            && sub_keysList.contains("goal");
                }
            }
            return false;
        }

        //check coinhive websocket payload pattern
        private boolean checkCoinHive(String payload) {
            try {
                JSONObject ws_obj = new JSONObject(payload);
                return checkJsonSchema(ws_obj);
            } catch (JSONException e) {
                // e.printStackTrace();
                return false;
            }
        }
    }
}

