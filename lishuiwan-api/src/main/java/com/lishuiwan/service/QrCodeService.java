package com.lishuiwan.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
public class QrCodeService {
  private static final int SIZE = 520;
  private static final MatrixToImageConfig COLORS = new MatrixToImageConfig(0xff102f29, 0xffffffff);

  public String asPngDataUrl(String content) {
    try {
      BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, SIZE, SIZE, Map.of(
          EncodeHintType.CHARACTER_SET, "UTF-8",
          EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
          EncodeHintType.MARGIN, 2
      ));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", output, COLORS);
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    } catch (WriterException | IOException e) {
      throw new IllegalStateException("生成会员二维码失败", e);
    }
  }
}
