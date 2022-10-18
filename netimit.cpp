#include <iostream>

using namespace std;

class AntennaSector {
private:
	float power;
	float center_azimuth;
	float width;

public:
	AntennaSector(float power, float azimuth, float width):
		power(power),
		center_azimuth(azimuth),
		width(width)
	{}

	float radiatedPower(float azimuth) {
		// power multiplied by antenna gain in given direction
		float angle = azimuth - center_azimuth;
		
		float G = 1.0;
		return power * G;
	}
};


class BaseStation {
private:
	float alt, lng;  // coordinates
	float power;     // dBm

public:
	BaseStation(float alt, float lng, float power):
		alt(alt), lng(lng), power(power)
	{}

	float receivedPower(float alt, float lng) {
		float p = 0;
		return p;
	}
};


struct Area {
	int xmin, xmax, ymin, ymax;
};


struct settings {
	int num_users;
	float usel_load;  // Erlangs. If divided by call duration,
	// get probability of call appearance at ineration



};



void simulate() {

}


int main(int argc, char **argv) {
	
	
	return 0;
}