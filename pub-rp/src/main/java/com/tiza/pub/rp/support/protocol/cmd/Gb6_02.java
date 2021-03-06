package com.tiza.pub.rp.support.protocol.cmd;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tiza.pub.air.model.Gb6Header;
import com.tiza.pub.air.model.Header;
import com.tiza.pub.air.model.PackUnit;
import com.tiza.pub.air.util.CommonUtil;
import com.tiza.pub.air.util.GpsCorrectUtil;
import com.tiza.pub.rp.support.protocol.Gb6DataProcess;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Description: Gb6_02
 * Author: DIYILIU
 * Update: 2019-07-03 09:51
 */

@Service
public class Gb6_02 extends Gb6DataProcess {

    public Gb6_02() {
        this.cmdId = 0x02;
    }

    @Override
    public void parse(byte[] content, Header header) {
        Gb6Header sixHeader = (Gb6Header) header;
        ByteBuf buf = Unpooled.copiedBuffer(content);
        // 解析时间
        fetchDate(sixHeader, buf);

        List<PackUnit> list = new ArrayList();
        while (buf.readableBytes() > 1) {
            int type = buf.readUnsignedByte();

            // OBD 信息
            if (0x01 == type) {
                PackUnit unit = new PackUnit(type);
                unit.setData(parseOBD(buf));
                list.add(unit);
            }
            // 数据流信息
            else if (0x02 == type) {
                PackUnit unit = new PackUnit(type);
                unit.setData( parseFlow(buf));
                list.add(unit);
            }
        }

        detach(sixHeader, list);
    }

    private Map parseOBD(ByteBuf buf) {
        int protocol = buf.readUnsignedByte();
        int mil = buf.readUnsignedByte();

        byte[] supStatus = new byte[2];
        buf.readBytes(supStatus);

        byte[] readyStatus = new byte[2];
        buf.readBytes(readyStatus);

        byte[] vinBytes = new byte[17];
        buf.readBytes(vinBytes);
        String vin = new String(vinBytes);

        byte[] softBytes = new byte[18];
        buf.readBytes(softBytes);
        String soft = new String(softBytes);

        byte[] cvnBytes = new byte[18];
        buf.readBytes(cvnBytes);
        String cvn = new String(cvnBytes);

        // 待定
        byte[] iuprBytes = new byte[36];
        buf.readBytes(iuprBytes);
        String iupr = new String(iuprBytes);

        // 故障信息
        List faults = Lists.newArrayList();
        int faultNum = buf.readUnsignedByte();
        if (faultNum > 0 && faultNum != 0xFE) {
            for (int i = 0; i < faultNum; i++) {
                int code = buf.readInt();
                faults.add(code);
            }
        }

        Map map = Maps.newHashMap();
        map.put("vin", vin);
        map.put("obdProtocol", protocol);
        map.put("mil", mil);
        map.put("supportStatus", CommonUtil.bytes2BinaryStr(supStatus));
        map.put("readyStatus", CommonUtil.bytes2BinaryStr(readyStatus));
        map.put("softId", soft);
        map.put("cvn", cvn);
        map.put("iupr", iupr);
        if (CollectionUtils.isNotEmpty(faults)) {
            map.put("faultInfo", faults);
        }

        return map;
    }

    private Map<String, Object> parseFlow(ByteBuf buf) {
        Map map = Maps.newHashMap();
        int speed = buf.readUnsignedShort();
        if (0xFFFF != speed) {
            map.put("speed", CommonUtil.keepDecimal(speed, 1 / 256, 3));
        }

        int pressure = buf.readUnsignedByte();
        if (0xFFFF != pressure) {
            map.put("airPressure", CommonUtil.keepDecimal(pressure, 0.5, 1));
        }

        // 扭矩
        int torque = buf.readUnsignedByte();
        if (0xFF != torque) {
            torque = torque - 125;
            map.put("torque", torque);
        }

        // 摩擦扭矩
        int frictionTq = buf.readUnsignedByte();
        if (0xFF != frictionTq) {
            frictionTq = frictionTq - 125;
            map.put("frictionTorque", frictionTq);
        }

        // 发动机转速
        int rpm = buf.readUnsignedShort();
        if (0xFFFF != rpm) {
            map.put("rpm", CommonUtil.keepDecimal(rpm, 0.125, 3));
        }

        // 燃料流量
        int ff = buf.readUnsignedShort();
        if (0xFFFF != ff) {
            map.put("fuelFlow", CommonUtil.keepDecimal(ff, 0.05, 2));
        }

        int upNox = buf.readUnsignedShort();
        if (0xFFFF != ff) {
            map.put("upNox", CommonUtil.keepDecimal(upNox, 0.05, 2));
        }

        int downNox = buf.readUnsignedShort();
        if (0xFFFF != ff) {
            map.put("downNox", CommonUtil.keepDecimal(downNox, 0.05, 2));
        }

        // 反应剂余量
        int reactant = buf.readUnsignedByte();
        if (0xFF != reactant) {
            map.put("reactantPercent", CommonUtil.keepDecimal(reactant, 0.4, 1));
        }

        // 进气量
        int inflow = buf.readUnsignedShort();
        if (0xFFFF != inflow) {
            map.put("airInflow", CommonUtil.keepDecimal(inflow, 0.05, 2));
        }

        int inTemp = buf.readUnsignedShort();
        if (0xFFFF != inTemp) {
            map.put("inTemp", CommonUtil.keepDecimal(inTemp, 0.03125, 5) - 273);
        }

        int outTemp = buf.readUnsignedShort();
        if (0xFFFF != outTemp) {
            map.put("outTemp", CommonUtil.keepDecimal(outTemp, 0.03125, 5) - 273);
        }

        // DFP 压差
        int dfpDif = buf.readUnsignedShort();
        if (0xFFFF != dfpDif) {
            map.put("dfpDif", CommonUtil.keepDecimal(dfpDif, 0.1, 1));
        }

        // 冷却液温度
        int coolantTemp = buf.readUnsignedByte();
        if (0xFF != coolantTemp) {
            map.put("coolantTemp", coolantTemp - 40);
        }

        // 油位
        int fuelLevel = buf.readUnsignedByte();
        if (0xFF != fuelLevel) {
            map.put("fuelLevel", CommonUtil.keepDecimal(fuelLevel, 0.4, 1));
        }

        // 定位状态
        int location = buf.readByte();

        //0:有效;1:无效
        int status = location & 0x01;
        int latDir = location & 0x02;
        int lngDir = location & 0x04;

        long lng = buf.readUnsignedInt();
        long lat = buf.readUnsignedInt();

        double lngD = CommonUtil.keepDecimal(lng * (lngDir == 0 ? 1 : -1), 0.000001, 6);
        double latD = CommonUtil.keepDecimal(lat * (latDir == 0 ? 1 : -1), 0.000001, 6);

        double[] latLng = GpsCorrectUtil.transform(latD, lngD);
        map.put("location", status);
        map.put("lng", lngD);
        map.put("lat", latD);
        map.put("enlng", latLng[1]);
        map.put("enlat", latLng[0]);

        long mileage = buf.readUnsignedInt();
        if (0xFFFFFFFF != mileage) {
            map.put("mileage", CommonUtil.keepDecimal(mileage, 0.1, 1));
        }

        return map;
    }
}
