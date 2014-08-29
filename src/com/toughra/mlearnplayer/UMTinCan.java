/*
 * Ustad Mobile (Micro Edition App)
 * 
 * Copyright 2011-2014 UstadMobile Inc. All rights reserved.
 * www.ustadmobile.com
 *
 * Ustad Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version with the following additional terms:
 * 
 * All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
 * LLC must be kept as they are in the original distribution.  If any new
 * screens are added you must include the Ustad Mobile logo as it has been
 * used in the original distribution.  You may not create any new
 * functionality whose purpose is to diminish or remove the Ustad Mobile
 * Logo.  You must leave the Ustad Mobile logo as the logo for the
 * application to be used with any launcher (e.g. the mobile app launcher).
 * 
 * If you want a commercial license to remove the above restriction you must
 * contact us and purchase a license without these restrictions.
 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 
 * Ustad Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
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
