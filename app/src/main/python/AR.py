import numpy as np
import argparse
import imutils
import sys
import cv2

# construct the argument parser and parse the arguments (input surface and the outputted AR)
ap = argparse.ArgumentParser()
ap.add_argument("-i", "--input_temp", required=True,
	help="path to input image containing the surface")
ap.add_argument("-s", "--source_temp", required=True,
	help="path to source image that will be put on input")
args = vars(ap.parse_args())

# load the input image from disk, resize it, and grab its spatial
# dimensions
print("[INFO] loading input image and source image...")
image = cv2.imread(args["image"])
image = imutils.resize(image, width=600)
(imgH, imgW) = image.shape[:2]
# load the source image from disk
source = cv2.imread(args["source"])