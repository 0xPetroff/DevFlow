import { Bar, BarChart, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

export interface CountDatum {
  label: string;
  value: number;
  /** A CSS colour, normally a theme variable, so the bar follows the light and dark palettes. */
  color: string;
}

interface CountBarChartProps {
  data: CountDatum[];
  caption: string;
}

/**
 * Horizontal bars, because the categories are words and a word reads better beside its bar than
 * rotated under a column. Every bar carries its category on the axis and its value at the tip, so
 * the colour only ever reinforces an identity the text has already given.
 */
export function CountBarChart({ data, caption }: CountBarChartProps) {
  const empty = data.every((datum) => datum.value === 0);

  return (
    <figure className="m-0">
      {empty ? (
        <p className="text-faint flex h-44 items-center justify-center text-sm">
          Nothing to chart yet.
        </p>
      ) : (
        <div className="h-44" aria-hidden="true">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={data}
              layout="vertical"
              margin={{ top: 4, right: 32, bottom: 4, left: 4 }}
              barCategoryGap={8}
            >
              <XAxis type="number" hide />
              <YAxis
                type="category"
                dataKey="label"
                width={96}
                tickLine={false}
                axisLine={false}
                tick={{ fill: 'var(--muted)', fontSize: 12 }}
              />
              <Tooltip cursor={{ fill: 'var(--elevated)' }} content={<ChartTooltip />} />
              <Bar dataKey="value" barSize={16} shape={RoundedBar} isAnimationActive={false}>
                <LabelList
                  dataKey="value"
                  position="right"
                  offset={8}
                  fill="var(--muted)"
                  fontSize={12}
                />
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* The same numbers as text: the chart itself is hidden from assistive technology, which
          would otherwise read a pile of unlabelled SVG. */}
      <figcaption className="sr-only">
        <table>
          <caption>{caption}</caption>
          <tbody>
            {data.map((datum) => (
              <tr key={datum.label}>
                <th scope="row">{datum.label}</th>
                <td>{datum.value}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </figcaption>
    </figure>
  );
}

interface BarShapeProps {
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  payload?: CountDatum;
}

/**
 * Rounded where the data ends and square against the baseline, so the bar's start is exactly on
 * the axis and only its tip is softened. Recharts' own radius prop rounds both ends.
 */
function RoundedBar({ x = 0, y = 0, width = 0, height = 0, payload }: BarShapeProps) {
  const radius = Math.min(4, width, height / 2);
  const right = x + width;
  const bottom = y + height;

  return (
    <path
      fill={payload?.color ?? 'var(--accent)'}
      d={`M${x},${y} H${right - radius} A${radius},${radius} 0 0 1 ${right},${y + radius} V${bottom - radius} A${radius},${radius} 0 0 1 ${right - radius},${bottom} H${x} Z`}
    />
  );
}

interface TooltipPayload {
  payload?: { payload?: CountDatum }[];
  active?: boolean;
}

function ChartTooltip({ active, payload }: TooltipPayload) {
  const datum = payload?.[0]?.payload;
  if (active !== true || !datum) {
    return null;
  }
  return (
    <div className="border-line bg-surface shadow-pop rounded-md border px-2.5 py-1.5 text-xs">
      <span className="text-muted">{datum.label}: </span>
      <span className="text-ink font-medium tabular-nums">{datum.value}</span>
    </div>
  );
}
