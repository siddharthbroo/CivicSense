from sentence_transformers import SentenceTransformer
import numpy as np


class EmbeddingService:

    def __init__(self):
        self.model = SentenceTransformer(
            "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
        )

    def generate_embedding(self, text: str):
        embedding = self.model.encode(text)

        return embedding.tolist()

    def calculate_similarity(self, text1: str, text2: str):

        embedding1 = self.model.encode(text1)
        embedding2 = self.model.encode(text2)

        similarity = np.dot(embedding1, embedding2) / (
                np.linalg.norm(embedding1) *
                np.linalg.norm(embedding2)
        )

        return float(similarity)

    def compare_complaints(
            self,
            title1: str,
            description1: str,
            summary1: str,
            category1: str,
            title2: str,
            description2: str,
            summary2: str,
            category2: str
    ):

        title_embedding1 = self.model.encode(title1)
        title_embedding2 = self.model.encode(title2)

        description_embedding1 = self.model.encode(description1)
        description_embedding2 = self.model.encode(description2)

        summary_embedding1 = self.model.encode(summary1)
        summary_embedding2 = self.model.encode(summary2)

        title_similarity = np.dot(
            title_embedding1,
            title_embedding2
        ) / (
                                   np.linalg.norm(title_embedding1) *
                                   np.linalg.norm(title_embedding2)
                           )

        description_similarity = np.dot(
            description_embedding1,
            description_embedding2
        ) / (
                                         np.linalg.norm(description_embedding1) *
                                         np.linalg.norm(description_embedding2)
                                 )

        summary_similarity = np.dot(
            summary_embedding1,
            summary_embedding2
        ) / (
                                     np.linalg.norm(summary_embedding1) *
                                     np.linalg.norm(summary_embedding2)
                             )

        category_match = (
                category1.strip().upper()
                == category2.strip().upper()
        )

        return {
            "title_similarity": float(title_similarity),
            "description_similarity": float(description_similarity),
            "summary_similarity": float(summary_similarity),
            "category_match": category_match
        }

    def calculate_address_similarity(
            self,
            address1: str,
            address2: str
    ):

        embedding1 = self.model.encode(address1)
        embedding2 = self.model.encode(address2)

        similarity = np.dot(
            embedding1,
            embedding2
        ) / (
                             np.linalg.norm(embedding1) *
                             np.linalg.norm(embedding2)
                     )

        return float(similarity)