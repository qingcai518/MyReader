package org.kaka.myreader.common;

import android.os.Environment;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AppConstants {
    //    public final static String BASE_URL = "http://192.168.1.103:80";
    public final static String BASE_URL = "http://myreader.main.jp";
    public final static String SERVER = BASE_URL + "/service.php";
    public final static String CHAPTER_SERVER = BASE_URL + "/chapterService.php?id=";
    public final static String USER_SERVER = BASE_URL + "/userService.php?userId=";
    public final static String USER_ADD_SERVER = BASE_URL + "/insertUser.php";
    public final static String USER_UPDATE_SERVER = BASE_URL + "/updateUser.php";
    public final static String LOAD_COMMIT_SERVER = BASE_URL + "/loadCommit.php?id=";
    public final static String ADD_COMMIT_SERVER = BASE_URL + "/insertCommit.php?";

    public final static String SDCARD_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();
    public final static String APP_DIR = Environment.getExternalStorageDirectory() + "/MyReader";
    public final static String APP_DOWNLOAD_DIR = APP_DIR + "/download";
    public final static String APP_PIC_DIR = APP_DIR + "/pics/";
    public final static String APP_WIFI_DIR = APP_DIR + "/wifi/";
    public final static String ID_PREFIX_IMPORT = "import_";
    public final static String ID_PREFIX_WIFI = "wifi_";
    public final static int API_LEVEL = android.os.Build.VERSION.SDK_INT;

    public final static Integer BUFFER_SIZE = 8192;
    public final static String[] TAG_ARRAY = {"myBooks", "cloudBooks", "profile"};
    //    public final static String TAG1 = "myBooks";
//    public final static String TAG2 = "cloudBooks";
//    public final static String TAG3 = "profile";
    public final static String CAPTURE_TAG1 = "capture";
    public final static String CAPTURE_TAG2 = "bookmark";
    public final static String CAPTURE_TAG3 = "note";
    public final static String FILE_SEARCH_TAG1 = "manual";
    public final static String FILE_SEARCH_TAG2 = "auto";
    public final static String PREFERENCE_NAME = "MyReaderPref";
    public final static int TYPE_ZH = 0;
    public final static int TYPE_TW = 1;
    public final static String SIMPLES = "爱碍袄肮罢坝摆摆办板帮宝报贝备笔币毕毙边变标表别宾卜补布才参惨蚕灿仓层产搀谗馋缠忏尝偿厂长床车彻陈尘衬唇称惩痴迟齿冲虫丑筹处触出础刍疮辞从聪丛窜达呆带担胆单当档党导灯邓敌籴递淀点电垫冬东冻栋动斗独断对队吨夺堕恶恶尔儿发发范矾飞奋粪坟风丰凤妇复复麸肤盖干干干赶个巩沟过构购谷顾雇刮挂关观冈广归龟柜国汉号合轰哄哄后胡护壶沪画划华怀坏欢环还回会秽汇汇伙获迹迹几机击际剂济挤积饥鸡鸡极继家价夹艰荐戋坚歼监见茧舰鉴鉴拣硷硷姜将奖浆桨酱讲胶借阶节疖秸杰尽尽紧仅进烬惊竞旧举剧据巨惧卷觉开克壳垦恳夸块矿亏昆昆捆困扩腊蜡来兰拦栏烂劳痨乐类累垒泪厘里礼厉励离历历隶俩帘联恋怜炼练粮两辆了疗辽猎临邻灵龄岭刘浏龙楼娄录陆虏卤卤卢庐泸芦炉乱仑罗屡虑滤驴麻马买卖迈麦脉猫蛮门黾么霉蒙蒙蒙梦弥弥面庙灭蔑亩难鸟恼脑拟酿聂镊疟宁农欧盘辟苹凭朴仆扑栖齐气弃启岂千迁佥签签牵纤蔷墙墙枪乔侨桥窍窃亲寝庆穷琼秋区曲趋权劝确让扰热认荣洒伞丧扫啬涩杀晒伤舍摄沈审渗声升升胜圣绳湿适时实势师兽属数术树书帅双松苏苏肃虽随岁孙笋它态台檯摊滩瘫坛坛叹叹汤誊体条椭粜铁听厅头图涂团团袜袜洼万弯网为为伪伪韦卫稳乌务无雾牺席系系戏习吓虾绣锈献咸显宪县向响乡协写胁泻亵衅兴须选旋悬学寻逊凶压亚哑艳艳严岩盐厌养痒样阳尧钥药页叶爷业医异义仪艺亿忆隐阴蝇应营拥佣踊涌痈优犹邮忧余鱼御吁郁与誉屿渊远园愿跃岳云运韵酝札札扎扎杂灾赃赃灶凿枣斋战占毡赵这折征症证郑只只帜职致制执滞质种众钟钟肿周昼朱筑烛注专庄壮装妆状桩准浊总纵钻";
    public final static String TRADITIONS = "愛礙襖骯罷壩擺襬辦闆幫寶報貝備筆幣畢斃邊變標錶彆賓蔔補佈纔參慘蠶燦倉層產攙讒饞纏懺嘗償廠長牀車徹陳塵襯脣稱懲癡遲齒衝蟲醜籌處觸齣礎芻瘡辭從聰叢竄達獃帶擔膽單當檔黨導燈鄧敵糴遞澱點電墊鼕東凍棟動鬥獨斷對隊噸奪墮噁惡爾兒發髮範礬飛奮糞墳風豐鳳婦復複麩膚蓋幹榦乾趕個鞏溝過構購穀顧僱颳掛關觀岡廣歸龜櫃國漢號閤轟閧鬨後鬍護壺滬畫劃華懷壞歡環還迴會穢匯彙夥獲蹟跡幾機擊際劑濟擠積飢雞鷄極繼傢價夾艱薦戔堅殲監見繭艦鑑鑒揀礆鹼薑將獎漿槳醬講膠藉階節癤稭傑儘盡緊僅進燼驚競舊舉劇據鉅懼捲覺開剋殼墾懇誇塊礦虧崐崑綑睏擴臘蠟來蘭攔欄爛勞癆樂類纍壘淚釐裏禮厲勵離歷暦隸倆簾聯戀憐煉練糧兩輛瞭療遼獵臨鄰靈齡嶺劉瀏龍樓婁錄陸虜滷鹵盧廬瀘蘆爐亂侖羅屢慮濾驢蔴馬買賣邁麥脈貓蠻門黽麼徾矇懞濛夢彌瀰麵廟滅衊畝難鳥惱腦擬釀聶鑷瘧寧農歐盤闢蘋憑樸僕撲棲齊氣棄啟豈韆遷僉籤簽牽縴薔牆墻槍喬僑橋竅竊親寢慶窮瓊鞦區麯趨權勸確讓擾熱認榮灑傘喪掃嗇澀殺曬傷捨攝瀋審滲聲昇陞勝聖繩濕適時實勢師獸屬數術樹書帥雙鬆蘇囌肅雖隨歲孫筍牠態臺颱攤灘癱壇罎嘆歎湯謄體條橢糶鐵聽廳頭圖塗團糰襪韤漥萬彎網為爲偽僞韋衛穩烏務無霧犧蓆係繫戲習嚇蝦繡銹獻醎顯憲縣嚮響鄉協寫脅瀉褻釁興鬚選鏇懸學尋遜兇壓亞啞艷豔嚴巖鹽厭養癢樣陽堯鑰藥頁葉爺業醫異義儀藝億憶隱陰蠅應營擁傭踴湧癰優猶郵憂餘魚禦籲鬱與譽嶼淵遠園願躍嶽雲運韻醞剳劄紥紮雜災贓髒竈鑿棗齋戰佔氈趙這摺徵癥證鄭祗隻幟職緻製執滯質種眾鐘鍾腫週晝誅築燭註專莊壯裝妝狀樁準濁總縱鑚";
    public final static int FONT_SIZE_MAX = 84;
    public final static int FONT_SIZE_MIN = 36;
    public final static int FONT_SIZE_DEFAULT = 60;

    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm", Locale.JAPAN);
    public static DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    public final static String PREF_KEY_COLOR = "color";
    public final static String PREF_KEY_FONTCOLOR = "fontColor";
    public final static String PREF_KEY_FONTSIZE = "fontSize";
    public final static String PREF_KEY_TYPE = "chineseType";
    public final static String PREF_KEY_DEFUALTTYPE = "isDefaultType";
    public final static String PREF_KEY_BOLD = "isBold";
    public final static String PREF_KEY_LIGHT = "isLighting";
    public final static String PREF_KEY_PLUS_ENABLE = "plusEnable";
    public final static String PREF_KEY_MINUS_ENABLE = "minusEnable";
    public final static String PREF_KEY_REDRINGINDEX = "indexOfRedRing";
    public final static String PREF_KEY_READ_ORDER = "order";
    public final static String PREF_KEY_LOGIN = "loginStatus";
    public final static String PREF_KEY_USERID = "userId";
    public final static String PREF_KEY_USERNAME = "userName";
    public final static String PREF_KEY_SEX = "sex";
    public final static String PREF_KEY_POINT = "point";
    public final static String PREF_KEY_USER_IMAGE = "userImage";

    public final static String BROADCAST_DOWNLOAD = "myreader.broadcast.download";
    public final static String BROADCAST_SENT_SMS = "myreader.broadcast.sendSMSAction";
    public final static String BROADCAST_DELIVERED_SMS = "myreader.broadcast.deliveredSMSAction";

    public final static int ORDER_READTIME = 0;
    public final static int ORDER_DOWNLOAD = 1;
    public final static int ORDER_BOOKNAME = 2;
    public final static int ORDER_AUTHOR = 3;

    public final static int AUTH_CODE_DIGITS = 5;
    public final static int AUTH_CODE_EXPIRE = 10 * 60 * 1000;  // ms

    public final static String KEY_REGIST_TYPE = "registType";
    public final static String KEY_INFO = "info";
    public final static int TYPE_PHONE = 0;
    public final static int TYPE_MAIL = 1;

    public static String CurrentAuthCode;
}