package com.lishuiwan.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeServiceTest {
  @Test
  void createsScannablePngDataUrl() throws Exception {
    String content = "lishuiwan://member-code?token=test-token";
    String result = new QrCodeService().asPngDataUrl(content);

    assertThat(result).startsWith("data:image/png;base64,");
    byte[] png = Base64.getDecoder().decode(result.substring(result.indexOf(',') + 1));
    assertThat(png).startsWith(0x89, 0x50, 0x4e, 0x47);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
        new BufferedImageLuminanceSource(ImageIO.read(new ByteArrayInputStream(png)))
    ));
    assertThat(new MultiFormatReader().decode(bitmap).getText()).isEqualTo(content);
  }
}
