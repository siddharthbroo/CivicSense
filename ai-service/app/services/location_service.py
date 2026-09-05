import math


class LocationService:

    EARTH_RADIUS_METERS = 6371000

    def calculate_distance(
            self,
            latitude1: float,
            longitude1: float,
            latitude2: float,
            longitude2: float
    ):

        lat1 = math.radians(latitude1)
        lat2 = math.radians(latitude2)

        delta_lat = math.radians(
            latitude2 - latitude1
        )

        delta_lon = math.radians(
            longitude2 - longitude1
        )

        a = (
                math.sin(delta_lat / 2) ** 2
                + math.cos(lat1)
                * math.cos(lat2)
                * math.sin(delta_lon / 2) ** 2
        )

        c = 2 * math.atan2(
            math.sqrt(a),
            math.sqrt(1 - a)
        )

        distance = self.EARTH_RADIUS_METERS * c

        return distance