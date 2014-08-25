/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toughra.mlearnplayer;
import org.json.me.*;


/** 
 *
 * @author mike
 */
public class UMTinCan {

    /**
     * Generate a JSON Object representing a TinCan statement for 'experience' a 
     * given page
     * 
     * @param tcParent TinCan ID URL prefix e.g. http://www.ustadmobile.com/xapi/pkgid
     * @param pageName Name of the page - e.g. intro (without .xml etc)
     * @param pageTitle Title of the page
     * @param pageLang language of the page for tincan purposes (e.g. en-US)
     * @param duration Duration viewed in ms
     * @param actor TinCan actor JSONObject
     * 
     * @return JSONObject representing the TinCan stmt, null if error
     */
    public static JSONObject makePageViewStmt(String tcParent, String pageName, String pageTitle, String pageLang, long duration, JSONObject actor) {
        JSONObject stmtObject = new JSONObject();
        String tinCanID = tcParent + "/" + pageName;
        try {
            stmtObject.put("actor", actor);
            
            JSONObject activityDef = new JSONObject();
            activityDef.put("type", "http://adlnet.gov/expapi/activities/module");
            activityDef.put("name", makeLangMapVal(pageLang, pageTitle));
            activityDef.put("description", makeLangMapVal(pageLang, pageTitle));
            
            JSONObject objectDef = new JSONObject();
            objectDef.put("id", tinCanID);
            objectDef.put("definition", activityDef);
            objectDef.put("objectType", "Activity");
            stmtObject.put("object", objectDef);

            
            JSONObject verbDef = new JSONObject();
            verbDef.put("id", "http://adlnet.gov/expapi/verbs/experienced");
            JSONObject verbDisplay = new JSONObject();
            verbDisplay.put("en-US", "experienced");
            verbDef.put("display", verbDisplay);
            stmtObject.put("verb", verbDef);
            
            String stmtDuration = MLearnUtils.format8601Duration(duration);
            JSONObject resultDef = new JSONObject();
            resultDef.put("duration", stmtDuration);
            stmtObject.put("result", resultDef);
            
            String totalStmtStr = stmtObject.toString();
            int x = 0;
        }catch(JSONException e) {
            EXEStrMgr.lg(181, "makePageViewStmt exception", e);
        }
        
        return stmtObject;
    }
    
    public static JSONObject makeLangMapVal(String lang, String value) {
        JSONObject retVal = new JSONObject();
        try {
            retVal.put(lang, value);
        }catch(JSONException e) {
            //this should never happen
            EXEStrMgr.lg(180, "Exception in makeLangMapVal",e);
        }
        
        return retVal;
    }
    
}
