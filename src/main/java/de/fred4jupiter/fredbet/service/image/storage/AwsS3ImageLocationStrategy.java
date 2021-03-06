package de.fred4jupiter.fredbet.service.image.storage;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fred4jupiter.fredbet.props.FredbetConstants;
import de.fred4jupiter.fredbet.service.image.BinaryImage;

public class AwsS3ImageLocationStrategy implements ImageLocationStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(AwsS3ImageLocationStrategy.class);

	private final AmazonS3ClientWrapper amazonS3ClientWrapper;

	public AwsS3ImageLocationStrategy(AmazonS3ClientWrapper amazonS3ClientWrapper) {
		this.amazonS3ClientWrapper = amazonS3ClientWrapper;
	}

	@Override
	public void saveImage(String imageKey, Long imageGroupId, byte[] imageBinary, byte[] thumbImageBinary) {
		LOG.debug("saving image in S3. imageKey={}, imageGroupId={}", imageKey, imageGroupId);
		amazonS3ClientWrapper.uploadImageFile(createKeyForImage(imageKey, imageGroupId), imageBinary);
		amazonS3ClientWrapper.uploadImageFile(createKeyForThumbnail(imageKey, imageGroupId), thumbImageBinary);
	}

	private String createKeyForThumbnail(String imageKey, Long imageGroupId) {
		return createFileKey(imageKey, imageGroupId, THUMBNAIL_PREFIX);
	}

	private String createKeyForImage(String imageKey, Long imageGroupId) {
		return createFileKey(imageKey, imageGroupId, IMAGE_PREFIX);
	}

	private String createFileKey(String imageKey, Long imageGroupId, String prefix) {
		return imageGroupId + "/" + prefix + imageKey + FredbetConstants.IMAGE_JPG_EXTENSION_WITH_DOT;
	}

	@Override
	public BinaryImage getImageByKey(String imageKey, Long imageGroupId) {
		LOG.debug("loading image from S3. imageKey={}, imageGroup={}", imageKey, imageGroupId);
		byte[] imageByte = amazonS3ClientWrapper.downloadFile(createKeyForImage(imageKey, imageGroupId));
		return new BinaryImage(imageKey, imageByte);
	}

	@Override
	public BinaryImage getThumbnailByKey(String imageKey, Long imageGroupId) {
		LOG.debug("loading thumbnail from S3. imageKey={}, imageGroup={}", imageKey, imageGroupId);
		byte[] imageByte = amazonS3ClientWrapper.downloadFile(createKeyForThumbnail(imageKey, imageGroupId));
		return new BinaryImage(imageKey, imageByte);
	}

	@Override
	public List<BinaryImage> findAllImages() {
		LOG.debug("loading all images from S3.");

		List<String> listFiles = amazonS3ClientWrapper.listFiles(FredbetConstants.IMAGE_JPG_EXTENSION_WITH_DOT);
		List<BinaryImage> files = amazonS3ClientWrapper.downloadAllFiles(listFiles);

		if (files.isEmpty()) {
			LOG.warn("Could not find any images in S3.");
			return Collections.emptyList();
		}

		return files;
	}

	@Override
	public void deleteImage(String imageKey, Long imageGroupId) {
		LOG.debug("deleteting image and thumbnail for imageKey={}, imageGroupId={}", imageKey, imageGroupId);
		amazonS3ClientWrapper.removeFile(createKeyForImage(imageKey, imageGroupId));
		amazonS3ClientWrapper.removeFile(createKeyForThumbnail(imageKey, imageGroupId));
	}

}
