package com.example.medaiassistant.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * 网络工具类
 * 提供获取本机IP地址等网络相关功能
 */
public class NetworkUtil {

    private static final Logger logger = LoggerFactory.getLogger(NetworkUtil.class);

    /**
     * 获取本机IP地址
     * 
     * @return 本机IP地址字符串，如果获取失败则返回null
     */
    public static String getLocalIpAddress() {
        try {
            // 首先尝试获取本机地址
            InetAddress localHost = InetAddress.getLocalHost();
            if (!localHost.isLoopbackAddress() && !localHost.isLinkLocalAddress()) {
                return localHost.getHostAddress();
            }

            // 如果本地地址是回环地址或链接本地地址，则遍历网络接口
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // 跳过回环接口和禁用的接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    // 跳过回环地址和链接本地地址
                    if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
                        continue;
                    }

                    // 返回第一个有效的IPv4地址
                    if (address.getAddress().length == 4) {
                        return address.getHostAddress();
                    }
                }
            }

            // 如果没有找到有效的网络接口地址，返回本地主机地址
            return localHost.getHostAddress();
        } catch (UnknownHostException | SocketException e) {
            logger.error("获取本机IP地址失败", e);
            return null;
        }
    }

    /**
     * 获取本机主机名
     * 
     * @return 本机主机名，如果获取失败则返回null
     */
    public static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("获取本机主机名失败", e);
            return null;
        }
    }
}
