import { apiClient } from '../client';
import { PointOfInterest } from '../../types';

export const poiApi = {
  list: (type?: string) => {
    const url = type ? `/points-of-interest?type=${type}` : '/points-of-interest';
    return apiClient.getAxiosInstance().get<PointOfInterest[]>(url);
  },
};