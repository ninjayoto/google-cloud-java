package com.google.gcloud.storage.contrib.nio;

import static com.google.common.truth.Truth.assertThat;
import static com.google.gcloud.storage.contrib.nio.CloudStorageOptions.withAcl;
import static com.google.gcloud.storage.contrib.nio.CloudStorageOptions.withCacheControl;
import static com.google.gcloud.storage.contrib.nio.CloudStorageOptions.withContentDisposition;
import static com.google.gcloud.storage.contrib.nio.CloudStorageOptions.withContentEncoding;
import static com.google.gcloud.storage.contrib.nio.CloudStorageOptions.withMimeType;
import static com.google.gcloud.storage.contrib.nio.CloudStorageOptions.withUserMetadata;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.gcloud.storage.Acl;
import com.google.gcloud.storage.testing.LocalGcsHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Unit tests for {@link CloudStorageFileAttributes}.
 */
@RunWith(JUnit4.class)
public class CloudStorageFileAttributesTest {

  private static final byte[] HAPPY = "(✿◕ ‿◕ )ノ".getBytes(UTF_8);

  private Path path;
  private Path dir;

  /** empty test storage and make sure we use it instead of the real GCS. Create a few paths. **/
  @Before
  public void before() {
    CloudStorageFileSystemProvider.setGCloudOptions(LocalGcsHelper.options());
    path = Paths.get(URI.create("gs://bucket/randompath"));
    dir = Paths.get(URI.create("gs://bucket/randompath/"));
  }

  @Test
  public void testCacheControl() throws Exception {
    Files.write(path, HAPPY, withCacheControl("potato"));
    assertThat(Files.readAttributes(path, CloudStorageFileAttributes.class).cacheControl().get())
        .isEqualTo("potato");
  }

  @Test
  public void testMimeType() throws Exception {
    Files.write(path, HAPPY, withMimeType("text/potato"));
    assertThat(Files.readAttributes(path, CloudStorageFileAttributes.class).mimeType().get())
        .isEqualTo("text/potato");
  }

  @Test
  public void testAcl() throws Exception {
    Acl acl = Acl.of(new Acl.User("serf@example.com"), Acl.Role.READER);
    Files.write(path, HAPPY, withAcl(acl));
    assertThat(Files.readAttributes(path, CloudStorageFileAttributes.class).acl().get())
        .contains(acl);
  }

  @Test
  public void testContentDisposition() throws Exception {
    Files.write(path, HAPPY, withContentDisposition("crash call"));
    assertThat(
            Files.readAttributes(path, CloudStorageFileAttributes.class).contentDisposition().get())
        .isEqualTo("crash call");
  }

  @Test
  public void testContentEncoding() throws Exception {
    Files.write(path, HAPPY, withContentEncoding("my content encoding"));
    assertThat(Files.readAttributes(path, CloudStorageFileAttributes.class).contentEncoding().get())
        .isEqualTo("my content encoding");
  }

  @Test
  public void testUserMetadata() throws Exception {
    Files.write(path, HAPPY, withUserMetadata("green", "bean"));
    assertThat(
            Files.readAttributes(path, CloudStorageFileAttributes.class)
                .userMetadata()
                .get("green"))
        .isEqualTo("bean");
  }

  @Test
  public void testIsDirectory() throws Exception {
    Files.write(path, HAPPY);
    assertThat(Files.readAttributes(path, CloudStorageFileAttributes.class).isDirectory())
        .isFalse();
    assertThat(Files.readAttributes(dir, CloudStorageFileAttributes.class).isDirectory()).isTrue();
  }

  @Test
  public void testIsRegularFile() throws Exception {
    Files.write(path, HAPPY);
    assertThat(Files.readAttributes(path, CloudStorageFileAttributes.class).isRegularFile())
        .isTrue();
    assertThat(Files.readAttributes(dir, CloudStorageFileAttributes.class).isRegularFile())
        .isFalse();
  }

  @Test
  public void testIsOther() throws Exception {
    Files.write(path, HAPPY);
    assertThat(Files.readAttributes(path, CloudStorageFileAttributes.class).isOther()).isFalse();
    assertThat(Files.readAttributes(dir, CloudStorageFileAttributes.class).isOther()).isFalse();
  }

  @Test
  public void testIsSymbolicLink() throws Exception {
    Files.write(path, HAPPY);
    assertThat(Files.readAttributes(path, CloudStorageFileAttributes.class).isSymbolicLink())
        .isFalse();
    assertThat(Files.readAttributes(dir, CloudStorageFileAttributes.class).isSymbolicLink())
        .isFalse();
  }

  @Test
  public void testEquals_equalsTester() throws Exception {
    Files.write(path, HAPPY, withMimeType("text/plain"));
    CloudStorageFileAttributes a1 = Files.readAttributes(path, CloudStorageFileAttributes.class);
    CloudStorageFileAttributes a2 = Files.readAttributes(path, CloudStorageFileAttributes.class);
    Files.write(path, HAPPY, withMimeType("text/potato"));
    CloudStorageFileAttributes b1 = Files.readAttributes(path, CloudStorageFileAttributes.class);
    CloudStorageFileAttributes b2 = Files.readAttributes(path, CloudStorageFileAttributes.class);
    new EqualsTester().addEqualityGroup(a1, a2).addEqualityGroup(b1, b2).testEquals();
  }

  @Test
  public void testFilekey() throws Exception {
    Files.write(path, HAPPY, withMimeType("text/plain"));
    Path path2 = Paths.get(URI.create("gs://bucket/anotherrandompath"));
    Files.write(path2, HAPPY, withMimeType("text/plain"));

    // diff files cannot have same filekey
    CloudStorageFileAttributes a1 = Files.readAttributes(path, CloudStorageFileAttributes.class);
    CloudStorageFileAttributes a2 = Files.readAttributes(path2, CloudStorageFileAttributes.class);
    assertThat(a1.fileKey()).isNotEqualTo(a2.fileKey());

    // same for directories
    CloudStorageFileAttributes b1 = Files.readAttributes(dir, CloudStorageFileAttributes.class);
    CloudStorageFileAttributes b2 =
        Files.readAttributes(
            Paths.get(URI.create("gs://bucket/jacket/")), CloudStorageFileAttributes.class);
    assertThat(a1.fileKey()).isNotEqualTo(b1.fileKey());
    assertThat(b1.fileKey()).isNotEqualTo(b2.fileKey());
  }

  @Test
  public void testNullness() throws Exception {
    Files.write(path, HAPPY);
    CloudStorageFileAttributes pathAttributes =
        Files.readAttributes(path, CloudStorageFileAttributes.class);
    CloudStorageFileAttributes dirAttributes =
        Files.readAttributes(dir, CloudStorageFileAttributes.class);
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(pathAttributes);
    tester.testAllPublicInstanceMethods(dirAttributes);
  }
}
