package com.ytycc;

import com.ytycc.dispatch.ForwardClient;
import com.ytycc.dispatch.ReceiveClient;
import com.ytycc.config.UserInfo;
import com.ytycc.strategy.keylock.SynchronizedStrategy;
import com.ytycc.strategy.processing.ParallelConsumptionStrategy;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.StringUtil;
import org.apache.commons.cli.*;

import java.util.Map;
import java.util.function.Consumer;


public class PenetrationClient {


    public static final int DEFAULT_SERVER_FORWARD_PORT = 7000;
    public static final int DEFAULT_REAL_SERVER_PORT = 8080;
    public static final String DEFAULT_SERVER_HOST = "localhost";

    public static void main(String[] args) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);

        CommandLine cmd = parseCommandLine(args);
        if (cmd == null) return;

        String host = getHost(cmd);
        int forwardPort = getPort(cmd, "fp", DEFAULT_SERVER_FORWARD_PORT, "请输入正确的远程服务器转发端口");
        int realPort = getPort(cmd, "rp", DEFAULT_REAL_SERVER_PORT, "请输入正确的本地服务端口");

        getOptionValue(cmd, "u", null, UserInfo::setUserName);
        getOptionValue(cmd, "p", null, UserInfo::setPassword);

        if (host == null || forwardPort < 0 || realPort < 0) {
            return;
        }

        ForwardClient.Config config = new ForwardClient.Config(realPort);
        ReceiveClient.create(host, forwardPort, config, new SynchronizedStrategy()).establish();
//        AnalyzeUtil.startReport();
    }


    private static CommandLine parseCommandLine(String[] args) {
        Options options = getOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                printHelp(options);
                return null;
            }
            return cmd;
        } catch (ParseException e) {
            System.err.println("解析命令行参数时出错: " + e.getMessage());
            printHelp(options);
            return null;
        }
    }

    private static String getHost(CommandLine cmd) {
        String host = cmd.getOptionValue("host", DEFAULT_SERVER_HOST);
        if (StringUtil.isNullOrEmpty(host)) {
            System.out.println("请输入正确的远程服务器ip");
            return null;
        }
        return host;
    }

    private static int getPort(CommandLine cmd, String option, int defaultPort, String errorMessage) {
        try {
            return Integer.parseInt(cmd.getOptionValue(option, String.valueOf(defaultPort)));
        } catch (NumberFormatException e) {
            System.out.println(errorMessage);
            return -1;
        }
    }


    private static String getOptionValue(CommandLine cmd, String option, String errorMessage, Consumer<String> setter) {
        String value = cmd.getOptionValue(option);
        if (StringUtil.isNullOrEmpty(value)) {
            if (!StringUtil.isNullOrEmpty(errorMessage)) {
                System.out.println(errorMessage);
            }
            return null;
        }
        setter.accept(value);
        return value;
    }


    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("PenetrationClient", options);
    }


    private static Options getOptions() {
        Options options = new Options();
        options.addOption("help", "help", false, "帮助信息");
        options.addOption("h", "host", true, "远程服务器ip");
        options.addOption("fp", "forwardPort", true, "远程服务器转发端口");
        //短opt有两位,不可 -rp=8080,只能-rp 8080 e.g. -rp 8080;事实上，本来短opt应该为单字符
        options.addOption("rp", "realPort", true, "本地服务端口,e.g. -rp 8080");
        //短opt只有一位，可 -p=123456或-p 123456,长opt都行
        options.addOption("p", "password", true, "密码");
        options.addOption("u", "user", true, "账号");
        return options;
    }

}
