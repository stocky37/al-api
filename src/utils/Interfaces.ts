import { Router } from "express";

export interface Settings {
    version: string;
    env: string;
    baseUrl: string;
}

export interface Controller {
    router: Router;
    settings: Settings;
}

export interface ConstructionData {
    time: string;
    ships: string[];
}