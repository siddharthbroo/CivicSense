import requests
import numpy as np
import torch

from PIL import Image
from io import BytesIO
from transformers import CLIPModel, CLIPProcessor


class ImageEmbeddingService:

    def __init__(self):

        self.processor = CLIPProcessor.from_pretrained(
            "openai/clip-vit-base-patch32"
        )

        self.model = CLIPModel.from_pretrained(
            "openai/clip-vit-base-patch32"
        )

        self.model.eval()

    def generate_image_embedding(self, image_url: str):

        response = requests.get(image_url)
        response.raise_for_status()

        image = Image.open(
            BytesIO(response.content)
        ).convert("RGB")

        inputs = self.processor(
            images=image,
            return_tensors="pt"
        )

        with torch.no_grad():

            vision_output = self.model.vision_model(
                **inputs
            )

            pooled_output = vision_output.pooler_output

            image_features = self.model.visual_projection(
                pooled_output
            )

        embedding = image_features.cpu().numpy()[0]

        return embedding.tolist()

    def calculate_image_similarity(
            self,
            embedding1,
            embedding2
    ):

        embedding1 = np.array(embedding1)
        embedding2 = np.array(embedding2)

        similarity = np.dot(
            embedding1,
            embedding2
        ) / (
                             np.linalg.norm(embedding1) *
                             np.linalg.norm(embedding2)
                     )

        return float(similarity)