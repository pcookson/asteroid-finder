export type NeoSummary = {
  id: string;
  name: string;
  isHazardous: boolean;
  diameterMinMeters: number;
  diameterMaxMeters: number;
  closeApproachTime: string;
  orbitingBody: string;
  missDistanceKm: number;
  missDistanceLunar: number;
  relativeVelocityKmPerSec: number;
};

